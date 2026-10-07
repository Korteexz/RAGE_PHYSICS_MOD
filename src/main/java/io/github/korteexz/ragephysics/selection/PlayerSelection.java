package io.github.korteexz.ragephysics.selection;

import net.minecraft.core.BlockPos;

public class PlayerSelection {

    // =========================
    // POSIÇÕES DA SELEÇÃO
    // =========================

    private BlockPos posA;
    private BlockPos posB;
    // Configuração apenas visual; não altera ticks ou física.
    private double timeScale = 1.0;
    private long revision;


    // =========================
    // DEFINIR POSIÇÃO
    // =========================

    public void select(BlockPos pos) {
        revision++;

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
        return posA != null && posB != null;
    }

    public double getTimeScale() {
        return timeScale;
    }

    public long getRevision() {
        return revision;
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
