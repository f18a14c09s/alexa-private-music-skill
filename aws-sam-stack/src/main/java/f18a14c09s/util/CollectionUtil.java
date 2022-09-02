package f18a14c09s.util;

import java.util.*;
import java.util.stream.*;

public class CollectionUtil {
    public static <E> ArrayList<E> asArrayList(E... elements) {
        return Optional.ofNullable(elements)
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .collect(ArrayList::new, List::add, List::addAll);
    }
}
