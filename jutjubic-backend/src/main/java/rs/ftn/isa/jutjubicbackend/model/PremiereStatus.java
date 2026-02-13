package rs.ftn.isa.jutjubicbackend.model;

public enum PremiereStatus {
    SCHEDULED,  // Video is scheduled for premiere, not yet started
    LIVE,       // Premiere is currently live
    ENDED       // Premiere has ended, video is now a regular video
}

