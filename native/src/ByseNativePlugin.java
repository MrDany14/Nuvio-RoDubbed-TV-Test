import java.util.*;
import com.lagradost.cloudstream3.metaproviders.TmdbProvider;

/** Nuvio's documented reflective plugin entry point, with no bundled runtime classes. */
public final class ByseNativePlugin {
    public void load() {}
    public List<TmdbProvider> getRegisteredMainAPIs(){return Collections.<TmdbProvider>singletonList(new ByseNativeProvider());}
    public List<Object> getRegisteredExtractorAPIs(){return Collections.emptyList();}
}
