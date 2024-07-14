package com.nlw.planner.trip;

import com.nlw.planner.activity.ActivityData;
import com.nlw.planner.activity.ActivityRequestPayload;
import com.nlw.planner.activity.ActivityResponse;
import com.nlw.planner.activity.ActivityService;
import com.nlw.planner.link.LinkData;
import com.nlw.planner.link.LinkRequestPayload;
import com.nlw.planner.link.LinkResponse;
import com.nlw.planner.link.LinkService;
import com.nlw.planner.participant.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {
    @Autowired
    private ParticipantService participantService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private TripRepository repository;

    // TRIPS

    @PostMapping
    public ResponseEntity<TripCreateResponse> createTrip(@RequestBody TripRequestPayload payload) {
        Trip newTrip = new Trip(payload);

        this.repository.save(newTrip);

        this.participantService.registerParticipantsToEvent(payload.emails_to_invite(), newTrip);

        return ResponseEntity.ok(new TripCreateResponse(newTrip.getId()));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<Trip> getTrip(@PathVariable UUID tripId) {
        Optional<Trip> trip = this.repository.findById(tripId);

        return trip.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{tripId}")
    public ResponseEntity<Trip> updateTrip(@PathVariable UUID tripId, @RequestBody TripRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(tripId);

        if (trip.isPresent()) {
            Trip rawTrip = trip.get();
            rawTrip.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setStartsAt(LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setDestination(payload.destination());

            this.repository.save(rawTrip);

            return ResponseEntity.ok(rawTrip);
        }

        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{tripId}/confirm")
    public ResponseEntity<Trip> confirmTrip(@PathVariable UUID tripId) {
        Optional<Trip> trip = this.repository.findById(tripId);

        if (trip.isPresent()) {
            Trip rawTrip = trip.get();
            rawTrip.setIsConfirmed(true);

            this.repository.save(rawTrip);
            this.participantService.triggerConfirmationEmailToParticipants(tripId);

            return ResponseEntity.ok(rawTrip);
        }

        return ResponseEntity.notFound().build();
    }

    // ACTIVITIES

    @PostMapping("/{tripId}/activities")
    public ResponseEntity<ActivityResponse> createActivity(@PathVariable UUID tripId, @RequestBody ActivityRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(tripId);

        if (trip.isPresent()) {
            Trip rawTrip = trip.get();

            ActivityResponse activityResponse = this.activityService.saveActivity(payload, rawTrip);

            return ResponseEntity.ok(activityResponse);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{tripId}/activities")
    public ResponseEntity<List<ActivityData>> getAllActivities(@PathVariable UUID tripId) {
        List<ActivityData> activityDataList = this.activityService.getAllActivitiesFromTrip(tripId);



        return ResponseEntity.ok(activityDataList);
    }

    // PARTICIPANTS

    @PostMapping("/{tripId}/invite")
    public ResponseEntity<ParticipantConfirmationResponse> inviteParticipant(@PathVariable UUID tripId, @RequestBody ParticipantRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(tripId);

        if (trip.isPresent()) {
            Trip rawTrip = trip.get();

            ParticipantConfirmationResponse participantResponse = this.participantService.registerParticipantToEvent(payload.email(), rawTrip);

            if (rawTrip.getIsConfirmed()) this.participantService.triggerConfirmationEmailToParticipant(payload.email());

            return ResponseEntity.ok(participantResponse);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{tripId}/participants")
    public ResponseEntity<List<ParticipantData>> getAllTripParticipants(@PathVariable UUID tripId) {
        List<ParticipantData> participantsList = this.participantService.getAllTripParticipants(tripId);
        return ResponseEntity.ok(participantsList);
    }

    // LINKS

    @PostMapping("/{tripId}/links")
    public ResponseEntity<LinkResponse> createLink(@PathVariable UUID tripId, @RequestBody LinkRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(tripId);

        if (trip.isPresent()) {
            Trip rawTrip = trip.get();
            LinkResponse linkResponse = this.linkService.saveLink(payload, rawTrip);
            return ResponseEntity.ok(linkResponse);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{tripId}/links")
    public ResponseEntity<List<LinkData>> getAllLinks(@PathVariable UUID tripId) {
        List<LinkData> linksList = this.linkService.getAllLinksFromTrip(tripId);
        return ResponseEntity.ok(linksList);
    }
}
