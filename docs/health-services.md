# Health Services integration boundary

The current watch flow uses the pure `WorkoutSession` state machine and manual taps. The next milestone should add a device adapter around Health Services `ExerciseClient`: start/pause/end a swimming pool exercise, request only supported metrics, and map actual distance, laps, strokes, and heart rate to separate recorded values. Never infer sensor values from the planned workout. Check capabilities and permissions at runtime, and test on pool hardware before claiming tracking accuracy.

Android's [ExerciseClient guide](https://developer.android.com/health-and-fitness/health-services/active-data) recommends ExerciseClient for active workouts. The [permissions guide](https://developer.android.com/health-and-fitness/health-services/permissions) documents OS-version-specific health permissions. No Health Services dependency or permissions are included in this manual prototype.
