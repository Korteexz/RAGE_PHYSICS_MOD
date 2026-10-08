package io.github.korteexz.ragephysics.timestamper.region;

/** Expected outcomes of a server-authoritative region operation. */
public enum RegionOperationStatus {
    SUCCESS,
    NO_SELECTION,
    INCOMPLETE_SELECTION,
    INVALID_DIMENSION,
    NOT_FOUND,
    NOT_OWNER,
    OVERLAP,
    INVALID_NAME,
    INVALID_TIME_SCALE,
    INVALID_MODE,
    INVALID_TARGETS,
    NO_CHANGE
}
