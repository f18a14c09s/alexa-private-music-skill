package f18a14c09s.integration.mp3;

import lombok.Getter;

public enum Mp3Parameter {
    MP3_FRAMESIZE_BYTES(Integer.class, "mp3.framesize.bytes"),
    MP3_FRAMERATE_FPS(Float.class, "mp3.framerate.fps");
    @Getter
    private final Class<?> targetClass;
    @Getter
    private final String value;

    Mp3Parameter(Class<?> targetClass, String value) {
        this.targetClass = targetClass;
        this.value = value;
    }
    /*
MP3 parameters.
mp3.version.mpeg [String], mpeg version : 1,2 or 2.5
mp3.version.layer [String], layer version 1, 2 or 3
mp3.version.encoding [String], mpeg encoding : MPEG1, MPEG2-LSF, MPEG2.5-LSF
mp3.channels [Integer], number of channels 1 : mono, 2 : stereo.
mp3.frequency.hz [Integer], sampling rate in hz.
mp3.bitrate.nominal.bps [Integer], nominal bitrate in bps.
mp3.length.bytes [Integer], length in bytes.
mp3.length.frames [Integer], length in frames.
mp3.framesize.bytes [Integer], framesize of the first frame. framesize is not constant for VBR streams.
mp3.framerate.fps [Float], framerate in frames per seconds.
mp3.header.pos [Integer], position of first audio header (or ID3v2 size).
mp3.vbr [Boolean], vbr flag.
mp3.vbr.scale [Integer], vbr scale.
mp3.crc [Boolean], crc flag.
mp3.original [Boolean], original flag.
mp3.copyright [Boolean], copyright flag.
mp3.padding [Boolean], padding flag.
mp3.mode [Integer], mode 0:STEREO 1:JOINT_STEREO 2:DUAL_CHANNEL 3:SINGLE_CHANNEL
mp3.id3tag.genre [String], ID3 tag (v1 or v2) genre.
mp3.id3tag.track [String], ID3 tag (v1 or v2) track info.
mp3.id3tag.encoded [String], ID3 tag v2 encoded by info.
mp3.id3tag.composer [String], ID3 tag v2 composer info.
mp3.id3tag.grouping [String], ID3 tag v2 grouping info.
mp3.id3tag.disc [String], ID3 tag v2 track info.
mp3.id3tag.publisher [String], ID3 tag v2 publisher info.
mp3.id3tag.orchestra [String], ID3 tag v2 orchestra info.
mp3.id3tag.length [String], ID3 tag v2 file length in seconds.
mp3.id3tag.v2 [InputStream], ID3v2 frames.
mp3.id3tag.v2.version [String], ID3v2 major version (2=v2.2.0, 3=v2.3.0, 4=v2.4.0).
mp3.shoutcast.metadata.key [String], Shoutcast meta key with matching value.
For instance :
mp3.shoutcast.metadata.icy-irc=#shoutcast
mp3.shoutcast.metadata.icy-metaint=8192
mp3.shoutcast.metadata.icy-genre=Trance Techno Dance
mp3.shoutcast.metadata.icy-url=http://www.di.fm
and so on ...
*/
}
