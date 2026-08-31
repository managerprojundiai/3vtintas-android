# Keep rules will be added only for dependencies that require reflection.
# The release build intentionally starts with the optimized Android defaults.

# Android desugars Java records below API 34. Jackson therefore uses the canonical constructor
# parameter names and private immutable fields for the mobile wire contract.
-keepattributes MethodParameters,Signature,*Annotation*
-keepclassmembers class br.com.tresvtintas.mobile.core.network.dto.** {
    <fields>;
    public <init>(...);
}
