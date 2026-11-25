package projet.saik.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class FlightDetails {

    @Column(name = "flight_departure_airport")
    private String departureAirport;
    @Column(name = "flight_departure_iata", length = 3)
    private String departureIata;
    @Column(name = "flight_arrival_airport")
    private String arrivalAirport;
    @Column(name = "flight_arrival_iata", length = 3)
    private String arrivalIata;
    @Column(name = "flight_stop_count")
    private Integer stopCount;
    @Column(name = "flight_stop_details", length = 2000)
    private String stopDetails;
    @Column(name = "flight_airline")
    private String airline;
    @Column(name = "flight_number")
    private String flightNumber;
    @Column(name = "flight_fare_type")
    private String fareType;
    @Column(name = "flight_cabin_baggage")
    private String cabinBaggage;
    @Column(name = "flight_cabin_baggage_weight")
    private Double cabinBaggageWeightKg;
    @Column(name = "flight_cabin_baggage_dimensions")
    private String cabinBaggageDimensions;
    @Column(name = "flight_personal_item")
    private String personalItem;
    @Column(name = "flight_personal_item_details")
    private String personalItemDetails;
    @Column(name = "flight_checked_baggage")
    private String checkedBaggage;
    @Column(name = "flight_checked_baggage_weight")
    private Double checkedBaggageWeightKg;
    @Column(name = "flight_checked_baggage_dimensions")
    private String checkedBaggageDimensions;
    @Column(name = "flight_modification_conditions")
    private String modificationConditions;
    @Column(name = "flight_cancellation_conditions")
    private String cancellationConditions;
    @Column(name = "flight_seat_choice")
    private String seatChoice;
    @Column(name = "flight_extra_baggage", length = 1000)
    private String extraBaggage;
    @Column(name = "flight_special_meal")
    private String specialMeal;
    @Column(name = "flight_optional_services", length = 1000)
    private String optionalServices;
    @Column(name = "flight_eticket_number", length = 13)
    private String eticketNumber;
    @Column(name = "flight_ticket_status")
    private String ticketStatus;
    @Column(name = "flight_departure_utc_offset")
    private Integer departureUtcOffsetMinutes;
    @Column(name = "flight_arrival_utc_offset")
    private Integer arrivalUtcOffsetMinutes;
    @Column(name = "flight_duration_minutes")
    private Long durationMinutes;

    public String getDepartureAirport() { return departureAirport; }
    public void setDepartureAirport(String departureAirport) { this.departureAirport = departureAirport; }
    public String getDepartureIata() { return departureIata; }
    public void setDepartureIata(String departureIata) { this.departureIata = departureIata; }
    public String getArrivalAirport() { return arrivalAirport; }
    public void setArrivalAirport(String arrivalAirport) { this.arrivalAirport = arrivalAirport; }
    public String getArrivalIata() { return arrivalIata; }
    public void setArrivalIata(String arrivalIata) { this.arrivalIata = arrivalIata; }
    public Integer getStopCount() { return stopCount; }
    public void setStopCount(Integer stopCount) { this.stopCount = stopCount; }
    public String getStopDetails() { return stopDetails; }
    public void setStopDetails(String stopDetails) { this.stopDetails = stopDetails; }
    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public String getFareType() { return fareType; }
    public void setFareType(String fareType) { this.fareType = fareType; }
    public String getCabinBaggage() { return cabinBaggage; }
    public void setCabinBaggage(String cabinBaggage) { this.cabinBaggage = cabinBaggage; }
    public Double getCabinBaggageWeightKg() { return cabinBaggageWeightKg; }
    public void setCabinBaggageWeightKg(Double cabinBaggageWeightKg) { this.cabinBaggageWeightKg = cabinBaggageWeightKg; }
    public String getCabinBaggageDimensions() { return cabinBaggageDimensions; }
    public void setCabinBaggageDimensions(String cabinBaggageDimensions) { this.cabinBaggageDimensions = cabinBaggageDimensions; }
    public String getPersonalItem() { return personalItem; }
    public void setPersonalItem(String personalItem) { this.personalItem = personalItem; }
    public String getPersonalItemDetails() { return personalItemDetails; }
    public void setPersonalItemDetails(String personalItemDetails) { this.personalItemDetails = personalItemDetails; }
    public String getCheckedBaggage() { return checkedBaggage; }
    public void setCheckedBaggage(String checkedBaggage) { this.checkedBaggage = checkedBaggage; }
    public Double getCheckedBaggageWeightKg() { return checkedBaggageWeightKg; }
    public void setCheckedBaggageWeightKg(Double checkedBaggageWeightKg) { this.checkedBaggageWeightKg = checkedBaggageWeightKg; }
    public String getCheckedBaggageDimensions() { return checkedBaggageDimensions; }
    public void setCheckedBaggageDimensions(String checkedBaggageDimensions) { this.checkedBaggageDimensions = checkedBaggageDimensions; }
    public String getModificationConditions() { return modificationConditions; }
    public void setModificationConditions(String modificationConditions) { this.modificationConditions = modificationConditions; }
    public String getCancellationConditions() { return cancellationConditions; }
    public void setCancellationConditions(String cancellationConditions) { this.cancellationConditions = cancellationConditions; }
    public String getSeatChoice() { return seatChoice; }
    public void setSeatChoice(String seatChoice) { this.seatChoice = seatChoice; }
    public String getExtraBaggage() { return extraBaggage; }
    public void setExtraBaggage(String extraBaggage) { this.extraBaggage = extraBaggage; }
    public String getSpecialMeal() { return specialMeal; }
    public void setSpecialMeal(String specialMeal) { this.specialMeal = specialMeal; }
    public String getOptionalServices() { return optionalServices; }
    public void setOptionalServices(String optionalServices) { this.optionalServices = optionalServices; }
    public String getEticketNumber() { return eticketNumber; }
    public void setEticketNumber(String eticketNumber) { this.eticketNumber = eticketNumber; }
    public String getTicketStatus() { return ticketStatus; }
    public void setTicketStatus(String ticketStatus) { this.ticketStatus = ticketStatus; }
    public Integer getDepartureUtcOffsetMinutes() { return departureUtcOffsetMinutes; }
    public void setDepartureUtcOffsetMinutes(Integer departureUtcOffsetMinutes) { this.departureUtcOffsetMinutes = departureUtcOffsetMinutes; }
    public Integer getArrivalUtcOffsetMinutes() { return arrivalUtcOffsetMinutes; }
    public void setArrivalUtcOffsetMinutes(Integer arrivalUtcOffsetMinutes) { this.arrivalUtcOffsetMinutes = arrivalUtcOffsetMinutes; }
    public Long getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Long durationMinutes) { this.durationMinutes = durationMinutes; }
}
