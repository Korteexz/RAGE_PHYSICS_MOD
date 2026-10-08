package io.github.korteexz.ragephysics.timestamper.region;

import java.util.Optional;

/** Semantic result returned to callers instead of using exceptions for normal user errors. */
public record RegionOperationResult(RegionOperationStatus status, TemporalRegion value) {
    public static RegionOperationResult success(TemporalRegion region) {
        return new RegionOperationResult(RegionOperationStatus.SUCCESS, region);
    }

    public static RegionOperationResult failure(RegionOperationStatus status) {
        if (status == RegionOperationStatus.SUCCESS) {
            throw new IllegalArgumentException("SUCCESS requires a region");
        }
        return new RegionOperationResult(status, null);
    }

    public boolean succeeded() {
        return status == RegionOperationStatus.SUCCESS;
    }

    public Optional<TemporalRegion> region() {
        return Optional.ofNullable(value);
    }
}
