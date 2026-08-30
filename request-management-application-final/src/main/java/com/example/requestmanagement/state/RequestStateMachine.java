package com.example.requestmanagement.state;

import com.example.requestmanagement.domain.RequestState;
import com.example.requestmanagement.exception.BusinessRuleViolationException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class RequestStateMachine {

    private final Map<RequestState, Set<RequestState>> transitions = Map.of(RequestState.CREATED, EnumSet.of(RequestState.VERIFIED, RequestState.DELETED), RequestState.VERIFIED, EnumSet.of(RequestState.ACCEPTED, RequestState.REJECTED), RequestState.ACCEPTED, EnumSet.of(RequestState.PUBLISHED, RequestState.REJECTED), RequestState.PUBLISHED, EnumSet.noneOf(RequestState.class), RequestState.REJECTED, EnumSet.noneOf(RequestState.class), RequestState.DELETED, EnumSet.noneOf(RequestState.class));

    public void validate(RequestState current, RequestState target) {

        if (current == null || target == null) {
            throw new BusinessRuleViolationException("Current state and target state are required");
        }

        if (current == target) {
            throw new BusinessRuleViolationException("Request is already in state " + current);
        }

        Set<RequestState> allowedStates = transitions.getOrDefault(current, Set.of());

        if (!allowedStates.contains(target)) {
            throw new BusinessRuleViolationException("Invalid state transition from " + current + " to " + target);
        }
    }
}