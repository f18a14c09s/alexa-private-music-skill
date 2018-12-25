package f18a14c09s.integration.alexa;

import f18a14c09s.integration.alexa.data.*;
import f18a14c09s.integration.alexa.music.catalog.CatalogDAO;
import f18a14c09s.integration.alexa.music.data.Content;
import f18a14c09s.integration.alexa.music.data.ContentActions;
import f18a14c09s.integration.alexa.music.data.RequestType;
import f18a14c09s.integration.alexa.music.data.ResolvedSelectionCriteria;
import f18a14c09s.integration.alexa.music.entities.*;
import f18a14c09s.integration.alexa.music.messagetypes.GetPlayableContentRequest;
import f18a14c09s.integration.alexa.music.messagetypes.GetPlayableContentResponse;
import f18a14c09s.integration.alexa.music.messagetypes.Response;

import java.io.IOException;
import java.util.*;
import java.util.stream.*;

public class GetPlayableContentSkill extends AbstractMusicSkill<GetPlayableContentRequest, Response<?>> {
    private CatalogDAO dao;

    public GetPlayableContentSkill() {
        super(GetPlayableContentRequest.class, new RequestType("Alexa.Media.Search", "GetPlayableContent"));
        try {
            dao = new CatalogDAO();
        } catch (IOException e) {
            getLogger().error("Unable to setup catalog DAO.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Response<?> invokeImpl(GetPlayableContentRequest request, Object context) {
        ResolvedSelectionCriteria criteria = Optional.ofNullable(request)
                .map(AbstractMessage::getPayload)
                .map(GetPlayableContentRequest.Payload::getSelectionCriteria)
                .orElse(null);
        if (criteria != null && criteria.getAttributes() != null) {
            List<ResolvedSelectionCriteria.BaseAttribute> artistFilter = criteria.getAttributes()
                    .stream()
                    .filter(attr -> attr.getType() == EntityType.ARTIST)
                    .collect(Collectors.toList());
            List<ResolvedSelectionCriteria.BaseAttribute> albumFilter = criteria.getAttributes()
                    .stream()
                    .filter(attr -> attr.getType() == EntityType.ALBUM)
                    .collect(Collectors.toList());
            List<ResolvedSelectionCriteria.BaseAttribute> songFilter = criteria.getAttributes()
                    .stream()
                    .filter(attr -> attr.getType() == EntityType.TRACK)
                    .collect(Collectors.toList());
            List<ResolvedSelectionCriteria.BaseAttribute> mediaType = criteria.getAttributes()
                    .stream()
                    .filter(attr -> attr.getType() == EntityType.MEDIA_TYPE)
                    .collect(Collectors.toList());
            List<ResolvedSelectionCriteria.BaseAttribute> sortType = criteria.getAttributes()
                    .stream()
                    .filter(attr -> attr.getType() == EntityType.SORT_TYPE)
                    .collect(Collectors.toList());
            if (!artistFilter.isEmpty()) {
                if (criteria.getAttributes().size() > artistFilter.size() + mediaType.size() + sortType.size()) {
                    return entityNotFound(request.getHeader().getMessageId(),
                            "Artist search filter seems to be invalid.");
                }
                return findEntity(request.getHeader().getMessageId(),
                        (ResolvedSelectionCriteria.BasicEntityAttribute) artistFilter.get(0),
                        Artist.class);
            } else if (!albumFilter.isEmpty()) {
                if (criteria.getAttributes().size() > albumFilter.size() + mediaType.size() + sortType.size()) {
                    return entityNotFound(request.getHeader().getMessageId(),
                            "Album search filter seems to be invalid.");
                }
                return findEntity(request.getHeader().getMessageId(),
                        (ResolvedSelectionCriteria.BasicEntityAttribute) albumFilter.get(0),
                        Album.class);
            } else if (!songFilter.isEmpty()) {
                if (criteria.getAttributes().size() > songFilter.size() + mediaType.size() + sortType.size()) {
                    return entityNotFound(request.getHeader().getMessageId(),
                            "Song search filter seems to be invalid.");
                }
                return findEntity(request.getHeader().getMessageId(),
                        (ResolvedSelectionCriteria.BasicEntityAttribute) songFilter.get(0),
                        Track.class);
            } else {
                return entityNotFound(request.getHeader().getMessageId(),
                        "Not sure how to handle the search criteria.");
            }
        } else {
            return entityNotFound(request.getHeader().getMessageId(), "No search criteria specified.");
        }
    }

    private <E extends BaseEntity> Response<?> findEntity(String requestId,
                                                          ResolvedSelectionCriteria.BasicEntityAttribute attr,
                                                          Class<E> targetClass) {
        E entity = dao.findEntity(targetClass, attr.getEntityId());
        if (entity == null) {
            return entityNotFound(requestId, attr);
        } else {
            return normalResponse(requestId, entity, ContentActions.browseableAndPlayable());
        }
    }

    private GetPlayableContentResponse normalResponse(String requestId, BaseEntity entity, ContentActions actions) {
        GetPlayableContentResponse retval = new GetPlayableContentResponse(requestId);
        Content content = new Content();
        content.setActions(actions);
        content.setId(entity.getId());
        content.setMetadata(entity.toMediaMetadata());
        GetPlayableContentResponse.Payload payload = new GetPlayableContentResponse.Payload();
        payload.setContent(content);
        retval.setPayload(payload);
        return retval;
    }

    private GenericErrorResponse entityNotFound(String requestId, ResolvedSelectionCriteria.BasicEntityAttribute attr) {
        GenericErrorResponse errorResponse = new GenericErrorResponse(requestId);
        errorResponse.setHeader(new MessageHeader());
        AbstractErrorResponse.Payload error = new AbstractErrorResponse.Payload();
        error.setType(ErrorResponseType.CONTENT_NOT_FOUND);
        error.setMessage(String.format("%s %s not found.", attr.getType(), attr.getEntityId()));
        errorResponse.setPayload(error);
        return errorResponse;
    }

    private GenericErrorResponse entityNotFound(String requestId, String errorMessage) {
        GenericErrorResponse errorResponse = new GenericErrorResponse(requestId);
        errorResponse.setHeader(new MessageHeader());
        AbstractErrorResponse.Payload error = new AbstractErrorResponse.Payload();
        error.setType(ErrorResponseType.CONTENT_NOT_FOUND);
        error.setMessage(errorMessage);
        errorResponse.setPayload(error);
        return errorResponse;
    }
}
