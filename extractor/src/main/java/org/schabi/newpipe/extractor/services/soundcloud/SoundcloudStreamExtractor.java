package org.schabi.newpipe.extractor.services.soundcloud;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.schabi.newpipe.extractor.*;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.stream.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.schabi.newpipe.extractor.utils.Utils.replaceHttpWithHttps;

public class SoundcloudStreamExtractor extends StreamExtractor {
    private JsonObject track;

    public SoundcloudStreamExtractor(StreamingService service, String url) {
        super(service, url);
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        track = SoundcloudParsingHelper.resolveFor(downloader, getOriginalUrl());

        String policy = track.getString("policy", "");
        if (!policy.equals("ALLOW") && !policy.equals("MONETIZE")) {
            throw new ContentNotAvailableException("Content not available: policy " + policy);
        }
    }

    @Nonnull
    @Override
    public String getCleanUrl() {
        return track.isString("permalink_url") ? replaceHttpWithHttps(track.getString("permalink_url")) : getOriginalUrl();
    }

    @Nonnull
    @Override
    public String getId() {
        return track.getInt("id") + "";
    }

    @Nonnull
    @Override
    public String getName() {
        return track.getString("title");
    }

    @Nonnull
    @Override
    public String getUploadDate() throws ParsingException {
        return SoundcloudParsingHelper.toDateString(track.getString("created_at"));
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() {
        return track.getString("artwork_url", "");
    }

    @Nonnull
    @Override
    public String getDescription() {
        return track.getString("description");
    }

    @Override
    public int getAgeLimit() {
        return NO_AGE_LIMIT;
    }

    @Override
    public long getLength() {
        return track.getNumber("duration", 0).longValue() / 1000L;
    }

    @Override
    public long getTimeStamp() throws ParsingException {
        return getTimestampSeconds("(#t=\\d{0,3}h?\\d{0,3}m?\\d{1,3}s?)");
    }

    @Override
    public long getViewCount() {
        return track.getNumber("playback_count", 0).longValue();
    }

    @Override
    public long getLikeCount() {
        return track.getNumber("favoritings_count", -1).longValue();
    }

    @Override
    public long getDislikeCount() {
        return -1;
    }

    @Nonnull
    @Override
    public String getUploaderUrl() {
        return SoundcloudParsingHelper.getUploaderUrl(track);
    }

    @Nonnull
    @Override
    public String getUploaderName() {
        return SoundcloudParsingHelper.getUploaderName(track);
    }

    @Nonnull
    @Override
    public String getUploaderAvatarUrl() {
        return SoundcloudParsingHelper.getAvatarUrl(track);
    }

    @Nonnull
    @Override
    public String getDashMpdUrl() {
        return "";
    }

    @Nonnull
    @Override
    public String getHlsUrl() throws ParsingException {
        return "";
    }

    @Override
    public List<AudioStream> getAudioStreams() throws IOException, ExtractionException {
        List<AudioStream> audioStreams = new ArrayList<>();
        Downloader dl = NewPipe.getDownloader();

        String apiUrl = "https://api.soundcloud.com/i1/tracks/" + urlEncode(getId()) + "/streams"
                + "?client_id=" + urlEncode(SoundcloudParsingHelper.clientId());

        String response = dl.download(apiUrl);
        JsonObject responseObject;
        try {
            responseObject = JsonParser.object().from(response);
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse json response", e);
        }

        String mp3Url = responseObject.getString("http_mp3_128_url");
        if (mp3Url != null && !mp3Url.isEmpty()) {
            audioStreams.add(new AudioStream(mp3Url, MediaFormat.MP3, 128));
        } else {
            throw new ExtractionException("Could not get SoundCloud's track audio url");
        }

        return audioStreams;
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<VideoStream> getVideoStreams() throws IOException, ExtractionException {
        return null;
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() throws IOException, ExtractionException {
        return null;
    }

    @Override
    @Nonnull
    public List<Subtitles> getSubtitlesDefault() throws IOException, ExtractionException {
        return Collections.emptyList();
    }

    @Override
    @Nonnull
    public List<Subtitles> getSubtitles(SubtitlesFormat format) throws IOException, ExtractionException {
        return Collections.emptyList();
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.AUDIO_STREAM;
    }

    @Override
    public StreamInfoItem getNextVideo() throws IOException, ExtractionException {
        return null;
    }

    @Override
    public StreamInfoItemsCollector getRelatedVideos() throws IOException, ExtractionException {
        StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());

        String apiUrl = "https://api-v2.soundcloud.com/tracks/" + urlEncode(getId()) + "/related"
                + "?client_id=" + urlEncode(SoundcloudParsingHelper.clientId());

        SoundcloudParsingHelper.getStreamsFromApi(collector, apiUrl);
        return collector;
    }


    @Override
    public String getErrorMessage() {
        return null;
    }
}
