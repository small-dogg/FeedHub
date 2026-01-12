package world.jerry.feedhub.api.infrastructure.opml;

public record ParsedOpmlEntry(
        String title,
        String xmlUrl,
        String htmlUrl
) {
}
