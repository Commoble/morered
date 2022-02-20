package commoble.morered.foundation.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class ConfigBase {

    public ForgeConfigSpec specification;
    protected int depth;
    protected List<MRValue<?, ?>> allValues;
    protected List<ConfigBase> children;

    protected void registerAll(final ForgeConfigSpec.Builder builder) {
        for (MRValue<?, ?> cValue : allValues)
            cValue.register(builder);
    }

    public void onLoad() {
        if (children != null)
            children.forEach(ConfigBase::onLoad);
    }

    public void onReload() {
        if (children != null)
            children.forEach(ConfigBase::onReload);
    }

    public abstract String getName();

    public class ConfigGroup extends MRValue<Boolean, ForgeConfigSpec.BooleanValue> {

        private final int groupDepth;
        private final String[] comment;

        public ConfigGroup(String name, int depth, String... comment) {
            super(name, builder -> null, comment);
            groupDepth = depth;
            this.comment = comment;
        }

        @Override
        public void register(ForgeConfigSpec.Builder builder) {
            if (depth > groupDepth)
                builder.pop(depth - groupDepth);
            depth = groupDepth;
            addComments(builder, comment);
            builder.push(getName());
            depth++;
        }

    }

    @FunctionalInterface
    protected interface IValueProvider<V, T extends ForgeConfigSpec.ConfigValue<V>>
            extends Function<ForgeConfigSpec.Builder, T> {
    }

    protected ConfigBool b(boolean current, String name, String... comment) {
        return new ConfigBool(name, current, comment);
    }

    protected ConfigDouble d(double current, double min, double max, String name, String... comment) {
        return new ConfigDouble(name, current, min, max, comment);
    }

    protected ConfigDouble d(double current, double min, String name, String... comment) {
        return d(current, min, Double.MAX_VALUE, name, comment);
    }

    protected ConfigGroup group(int depth, String name, String... comment) {
        return new ConfigGroup(name, depth, comment);
    }

    public class MRValue<V, T extends ForgeConfigSpec.ConfigValue<V>> {
        protected ForgeConfigSpec.ConfigValue<V> value;
        protected String name;
        private final IValueProvider<V, T> provider;

        public MRValue(String name, IValueProvider<V, T> provider, String... comment) {
            this.name = name;
            this.provider = builder -> {
                addComments(builder, comment);
                return provider.apply(builder);
            };
            if (allValues == null)
                allValues = new ArrayList<>();
            allValues.add(this);
        }

        public void addComments(ForgeConfigSpec.Builder builder, String... comment) {
            if (comment.length > 0) {
                String[] comments = new String[comment.length + 1];
                comments[0] = " ";
                System.arraycopy(comment, 0, comments, 1, comment.length);
                builder.comment(comments);
            } else
                builder.comment(" ");
        }

        public void register(ForgeConfigSpec.Builder builder) {
            value = provider.apply(builder);
        }

        public V get() {
            return value.get();
        }

        public void set(V value) {
            this.value.set(value);
        }

        public String getName() {
            return name;
        }
    }

    public class ConfigBool extends MRValue<Boolean, ForgeConfigSpec.BooleanValue> {

        public ConfigBool(String name, boolean def, String... comment) {
            super(name, builder -> builder.define(name, def), comment);
        }
    }

    public class ConfigDouble extends MRValue<Double, ForgeConfigSpec.DoubleValue> {

        public ConfigDouble(String name, double current, double min, double max, String... comment) {
            super(name, builder -> builder.defineInRange(name, current, min, max), comment);
        }

        public double getD() {
            return get().doubleValue();
        }
    }
}
