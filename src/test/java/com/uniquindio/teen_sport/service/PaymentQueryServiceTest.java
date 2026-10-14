package com.uniquindio.teen_sport.service;

import com.uniquindio.teen_sport.entity.PaymentHistory;
import com.uniquindio.teen_sport.entity.RoleName;
import com.uniquindio.teen_sport.exception.BusinessException;
import com.uniquindio.teen_sport.exception.ErrorCode;
import com.uniquindio.teen_sport.exception.FieldValidationException;
import com.uniquindio.teen_sport.repository.PaymentFilter;
import com.uniquindio.teen_sport.repository.PaymentHistoryRepository;
import com.uniquindio.teen_sport.security.AuthenticatedUser;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentQueryServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);

    private final PaymentHistoryRepository paymentRepository = mock(PaymentHistoryRepository.class);
    private final PaymentQueryService service = new PaymentQueryService(paymentRepository, new AthleteAccessPolicy());

    @Test
    @SuppressWarnings("unchecked")
    void listsOrderedByMostRecentPaymentWithPageMetadata() {
        Page<PaymentHistory> empty = new PageImpl<>(List.of(), PageRequest.of(1, 5), 7);
        when(paymentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(empty);

        var page = service.list(new AuthenticatedUser(1, RoleName.ADMIN, null, null),
                new PaymentFilter(9, 5, FROM, TO, null), 1, 5);

        assertThat(page.totalElements()).isEqualTo(7);
        assertThat(page.page()).isEqualTo(1);
        verify(paymentRepository).findAll(any(Specification.class), eq(PageRequest.of(1, 5, PaymentQueryService.ORDER)));
    }

    @Test
    void reversedDateRangeIsAValidationError() {
        FieldValidationException failure = catchThrowableOfType(FieldValidationException.class,
                () -> service.list(new AuthenticatedUser(1, RoleName.ADMIN, null, null),
                        new PaymentFilter(null, null, TO, FROM, null), 0, 20));

        assertThat(failure.getField()).isEqualTo("from");
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void coachesCannotListPayments() {
        BusinessException failure = catchThrowableOfType(BusinessException.class,
                () -> service.list(new AuthenticatedUser(4, RoleName.COACH, null, 40),
                        new PaymentFilter(null, null, null, null, null), 0, 20));

        assertThat(failure.getCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
        verifyNoInteractions(paymentRepository);
    }
}
