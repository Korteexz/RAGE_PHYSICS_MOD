package io.github.korteexz.ragephysics.selection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.UUID;

public class PlayerSelection {

    // =========================
    // POSIÇÕES DA SELEÇÃO
    // =========================

    private BlockPos posA;
    private BlockPos posB;
    private ResourceKey<Level> dimension;
    private double timeScale = 1.0;
    private long revision;
    private UUID regionId;


    // =========================
    // DEFINIR POSIÇÃO
    // =========================

    public void select(ResourceKey<Level> dimension, BlockPos pos) {
        revision++;

        // Nunca completa A/B entre dimensões diferentes.
        if (!dimension.equals(this.dimension)) {
            this.dimension = dimension;
            posA = pos.immutable();
            posB = null;
            timeScale = 1.0;
            regionId = null;
            return;
        }

        // Primeiro clique:
        // cria a posição A.
        if (posA == null) {
            posA = pos.immutable();
            return;
        }

        // Segundo clique:
        // cria a posição B.
        if (posB == null) {
            posB = pos.immutable();
            return;
        }

        // Terceiro clique:
        // começa uma seleção nova.
        posA = pos.immutable();
        posB = null;
        timeScale = 1.0;
        regionId = null;
    }


    // =========================
    // LEITURA
    // =========================

    public BlockPos getPosA() {
        return posA;
    }

    public BlockPos getPosB() {
        return posB;
    }


    // =========================
    // ESTADO
    // =========================

    public boolean hasPosA() {
        return posA != null;
    }

    public boolean hasPosB() {
        return posB != null;
    }

    public boolean isComplete() {
        return dimension != null && posA != null && posB != null;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public boolean isInDimension(ResourceKey<Level> dimension) {
        return dimension.equals(this.dimension);
    }

    /** Coordenadas de blocos inclusivas, como a caixa desenhada pelo preview. */
    public boolean contains(ResourceKey<Level> dimension, BlockPos pos) {
        return isComplete() && isInDimension(dimension)
                && pos.getX() >= Math.min(posA.getX(), posB.getX()) && pos.getX() <= Math.max(posA.getX(), posB.getX())
                && pos.getY() >= Math.min(posA.getY(), posB.getY()) && pos.getY() <= Math.max(posA.getY(), posB.getY())
                && pos.getZ() >= Math.min(posA.getZ(), posB.getZ()) && pos.getZ() <= Math.max(posA.getZ(), posB.getZ());
    }

    public double getTimeScale() {
        return timeScale;
    }

    public long getRevision() {
        return revision;
    }

    public UUID getRegionId() {
        return regionId;
    }

    public void bindRegion(UUID regionId) {
        this.regionId = regionId;
    }

    /** Consumes the current spatial draft after a successful explicit create. */
    public void clear() {
        revision++;
        posA = null;
        posB = null;
        dimension = null;
        timeScale = 1.0;
        regionId = null;
    }

    /** Rejeita telas antigas e valores inválidos antes de mudar o estado. */
    public boolean updateTimeScale(long expectedRevision, double value) {
        if (!isComplete() || revision != expectedRevision || !TimeScaleVisuals.isValid(value)) {
            return false;
        }
        timeScale = value;
        return true;
    }
}
