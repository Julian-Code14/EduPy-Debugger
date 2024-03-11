package de.code14.edupydebugger;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * @author julian
 * @version 1.0
 * @since 11.03.24
 */
public class MyBundle extends DynamicBundle {

    @NonNls
    private static final String BUNDLE = "messages.MyBundle";
    private static final MyBundle INSTANCE = new MyBundle();

    public MyBundle() {
        super(BUNDLE);
    }

    @NotNull
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return INSTANCE.getMessage(BUNDLE, key, params);
    }

    // Assuming you want to keep the messagePointer method, but its usage is not directly translatable to Java as it is Kotlin specific.
    // If you need similar functionality in Java, you might need to manually implement or adjust based on your use case.

}
