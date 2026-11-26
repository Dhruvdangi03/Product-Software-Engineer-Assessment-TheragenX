package com.theragenx.intake.api;

/** How many fields of a document a reviewer has decided on, out of all fields of all its cases. */
public record ReviewProgress(int reviewed, int total) {
}
