import java.util.*;
import com.lagradost.cloudstream3.metaproviders.TmdbProvider;
public final class RoDubbedPlugin {
    public void load(){}
    public List<TmdbProvider> getRegisteredMainAPIs(){return Collections.<TmdbProvider>singletonList(new RoDubbedProvider());}
    public List<Object> getRegisteredExtractorAPIs(){return Collections.emptyList();}
}
