package com.pedromorago.spintrainer.shared.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserIdTest {

    /** The id is the JWT's sub, and that is how it is written to the database and the logs. */
    @Test
    void isTheSubjectUuid() {
        UUID sub = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

        assertThat(new UserId(sub)).hasToString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
        assertThatThrownBy(() -> new UserId(null)).isInstanceOf(NullPointerException.class);
    }
}
