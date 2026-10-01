package com.jerzymaj.file_researcher_backend.DTOs;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public record Progress(AtomicLong bytesProcessed, AtomicInteger lastPercent, long totalSize) {
}
