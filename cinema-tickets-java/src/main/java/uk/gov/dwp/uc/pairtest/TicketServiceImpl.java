package uk.gov.dwp.uc.pairtest;

import java.util.Arrays;
import java.util.List;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidInputException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.MaxTicketsException;
import uk.gov.dwp.uc.pairtest.exception.MoreInfantsException;
import uk.gov.dwp.uc.pairtest.exception.NoAdultException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private static final int MAX_TICKETS = 20;

    private static final int PRICE_CHILD_TICKET  = 10;
    private static final int PRICE_ADULT_TICKET  = 20;
    private SeatReservationService seatReservationService;
    private TicketPaymentService ticketPaymentService;

    public TicketServiceImpl(SeatReservationService seatReservationService, TicketPaymentService ticketPaymentService) {
        this.seatReservationService = seatReservationService;
        this.ticketPaymentService = ticketPaymentService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        validateAccountId(accountId);
        List<TicketTypeRequest> ticketTypeRequestList = Arrays.asList(ticketTypeRequests);
        validateTicketRequest(ticketTypeRequestList);
        //Assumed a business rule one adult can sit one infant only.
        int adults = ticketTypeRequestList.stream().filter(i -> i.getTicketType() == TicketTypeRequest.Type.ADULT).mapToInt(i -> i.getNoOfTickets()).sum();
        int infants = ticketTypeRequestList.stream().filter(i -> i.getTicketType() == TicketTypeRequest.Type.CHILD).mapToInt(i -> i.getNoOfTickets()).sum();
        validateInfantNumbers(adults, infants);
        int seats = getSeatsTobReserved(ticketTypeRequestList);
        int price = calculatePayment(ticketTypeRequestList);

        seatReservationService.reserveSeat(accountId, seats);
        ticketPaymentService.makePayment(accountId, price);

    }

    private int getSeatsTobReserved(List<TicketTypeRequest> ticketTypeRequests) {

        return ticketTypeRequests.stream()
                    .filter( i -> i.getTicketType() != TicketTypeRequest.Type.INFANT)
                    .mapToInt( i -> i.getNoOfTickets())
                    .sum();
    }

    private void validateTicketRequest(List<TicketTypeRequest> ticketTypeRequests) {

        if (ticketTypeRequests == null || ticketTypeRequests.size() == 0 )  {
            throw new InvalidInputException();
        }
        if (! ticketTypeRequests
                    .stream()
                    .anyMatch( i -> (i.getTicketType() == TicketTypeRequest.Type.ADULT && i.getNoOfTickets() > 0))) {
            throw new NoAdultException();
        }

        if (ticketTypeRequests
                    .stream()
                    .mapToInt( i -> i.getNoOfTickets())
                    .sum() > MAX_TICKETS) {
            throw new MaxTicketsException();
        }
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidInputException();
        }
    }

    private int calculatePayment(List<TicketTypeRequest> ticketTypeRequests) {
        return ticketTypeRequests.stream()
                .filter( i -> i.getTicketType() != TicketTypeRequest.Type.INFANT)
                .mapToInt( i -> i.getNoOfTickets() * getPrice(i.getTicketType()))
                .sum();
    }

    private int getPrice(TicketTypeRequest.Type ticketType) {
        if (ticketType == TicketTypeRequest.Type.ADULT) {
            return 20;
        } else if(ticketType == TicketTypeRequest.Type.CHILD) {
            return 10;
        } else {
            return 0;
        }
    }

    //Assumed a business rule one adult can sit one infant only.
    private void validateInfantNumbers(int adults, int infants) {

        if (adults < infants) {
            throw new MoreInfantsException();
        }
    }

}
