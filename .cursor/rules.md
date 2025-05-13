# Cursor AI Usage Rules

## Code Generation and Style Guidelines

1. All code should follow modern Kotlin practices, utilizing Kotlin DSL and extension functions where appropriate.
2. Use MVVM pattern with Android Architecture Components (ViewModel, LiveData, Room, etc.).
3. When using Jetpack Compose, follow Material 3 design system by default.
4. All variables and functions must have clear names and follow Kotlin naming conventions.
5. All UI components must support dark mode.
6. All resources such as strings, colors, and dimensions should be defined in appropriate resource files.

## Performance and Optimization

1. Heavy operations should be processed in coroutines or workers, not on the main thread.
2. Implement asynchronous operations using coroutines, leveraging Flow or LiveData.
3. Manage resources with lifecycle considerations to prevent memory leaks.
4. Network requests should include caching strategies.
5. Use Coil or Glide libraries for image loading.

## Security and Data Handling

1. Store sensitive user data in EncryptedSharedPreferences or secure storage.
2. Manage sensitive information like API keys via local.properties or BuildConfig.
3. Only allow HTTPS for network communications.
4. Always validate and properly escape user input.
5. Use Room or parameterized queries for database operations to prevent SQL injection.

## Testing and Quality Assurance

1. All business logic should have unit tests.
2. Test UI components using Espresso or Compose Testing.
3. Regularly profile for memory leaks and performance bottlenecks.
4. Minimize lint warnings and pass static code analysis.

## Accessibility

1. All UI elements must be compatible with TalkBack.
2. Color contrast should meet WCAG standards.
3. All clickable elements should have a minimum touch target of 48dp x 48dp.
4. All images must include content descriptions.

## Version Control and Documentation

1. Git commit messages should clearly describe features or fixes.
2. Write KDoc documentation for complex algorithms or business logic.
3. Thoroughly document public APIs and classes.
4. Use semantic versioning scheme for version management.

## Dependency Management

1. Use the latest stable versions of libraries when possible.
2. Dependencies between app modules should be clear and avoid circular dependencies.
3. Verify licenses and maintenance status before using third-party libraries.
4. Optimize app size by using only necessary dependencies.

## Naver Maps SDK Integration

1. When implementing map features, reference and follow examples from the official Naver Maps SDK repository: https://github.com/navermaps/android-map-sdk
2. Follow Naver Maps usage guidelines and best practices for marker management, clustering, and custom overlays.
3. Implement proper lifecycle management for NMapView components.
4. Store Naver Maps API keys securely using the recommended BuildConfig approach.
5. Use vector drawables for map markers when possible to support different screen densities.
6. Implement appropriate error handling for map loading and location services.
7. Cache map data appropriately to reduce network usage and improve performance.
8. Follow the Naver Maps SDK versioning recommendations for compatibility.