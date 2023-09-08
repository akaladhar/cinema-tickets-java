package uk.gov.dwp.uc.pairtest;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidInputException;
import uk.gov.dwp.uc.pairtest.exception.MaxTicketsException;
import uk.gov.dwp.uc.pairtest.exception.MoreInfantsException;
import uk.gov.dwp.uc.pairtest.exception.NoAdultException;

@RunWith(MockitoJUnitRunner.class)
public class TicketServiceTest {

    @Mock
    private TicketPaymentService ticketPaymentService;
    @Mock
    private SeatReservationService seatReservationService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private static final long accountID = 1l;

    @Test(expected = InvalidInputException.class)
    public void testWithAccountIdNull() {
        ticketService.purchaseTickets(null, null);
    }

    @Test(expected = InvalidInputException.class)
    public void testWithAccountIdZero() {
        ticketService.purchaseTickets(0l, null);
    }

    @Test(expected = NoAdultException.class)
    public void testTicketsWithoutAdult() {

        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(accountID, childTickets, infantTickets);
    }

    @Test(expected = MoreInfantsException.class)
    public void testTicketsExcessInfants() {

        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 10);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 5);
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 5);

        ticketService.purchaseTickets(accountID, childTickets, infantTickets, adultTickets);
    }

    @Test(expected = MaxTicketsException.class)
    public void testTicketsExceedsMaxTicketsAllowed() {

        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 5);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 5);
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 16);

        ticketService.purchaseTickets(accountID, childTickets, infantTickets, adultTickets);
    }

    @Test
    public void testSeatsCalculationService() {

        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);

        ticketService.purchaseTickets(accountID, childTickets, infantTickets, adultTickets);

        verify(seatReservationService, times(1)).reserveSeat(accountID, 4);
    }

    @Test
    public void testInvokeTicketPaymentService() {

        TicketTypeRequest childTickets = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantTickets = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);
        TicketTypeRequest adultTickets = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);

        ticketService.purchaseTickets(accountID, childTickets, infantTickets, adultTickets);

        verify(ticketPaymentService, times(1)).makePayment(accountID, 60);
    }
}
