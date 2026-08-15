package com.example.fandoom_backend.lore.entity;

// EventParticipant.participantId'nin hangi tabloya referans verdiğini belirtir.
// CHARACTER cross-module (person/), GROUP aynı modül içi — ikisi de tutarlılık
// için ID-only tutulur (GroupAssignment.taggableId deseniyle aynı).
public enum ParticipantType {
    CHARACTER, GROUP
}
