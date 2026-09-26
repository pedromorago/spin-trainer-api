package com.pedromorago.spintrainer.range.application.port.in;

import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.util.List;
import java.util.Map;

/** Rangos personalizados: solo los escribe el Explorer (ADR-0012), con concurrencia optimista (ADR-0013). */
public interface ManageUserRanges {

    List<Range> all(UserId user);

    /** @throws com.pedromorago.spintrainer.shared.kernel.DomainException NOT_FOUND si la combinación no existe o no hay rango */
    Range get(UserId user, SituationKey situation, Stack stack);

    /**
     * Crea ({@code version} 0) o reemplaza la versión {@code version}. Las manos con la acción implícita se descartan.
     *
     * @throws com.pedromorago.spintrainer.shared.kernel.DomainException VALIDATION, NOT_FOUND o CONFLICT (la versión no
     *     es la actual: otra pestaña o dispositivo lo cambió)
     */
    SavedRange save(UserId user, SituationKey situation, Stack stack, Map<String, Action> hands, int version);

    /** Idempotente: vuelve a aplicar el de referencia. */
    void delete(UserId user, SituationKey situation, Stack stack);

    record SavedRange(Range range, boolean created) {}
}
