package br.unb.cic.reach.common.writer.json;

import java.util.List;
import java.util.stream.Collectors;

import br.unb.cic.reach.common.model.ReachClass;

/**
 * JSON representation of a class with reachability information.
 */
public class ReachClassJson {
    private final String className;
    private final String componentType;
    private final boolean isMainComponent;
    private final List<ReachMethodJson> methods;
    
    public ReachClassJson(ReachClass reachClass) {
        this.className = reachClass.getClassName();
        this.componentType = reachClass.getComponentType().getDisplayName();
        this.isMainComponent = reachClass.isMainComponent();
        this.methods = reachClass.getMethods().stream()
                .map(ReachMethodJson::new)
                .collect(Collectors.toList());
    }
    
    // Getters for JSON serialization
    public String getClassName() { return className; }
    public String getComponentType() { return componentType; }
    public boolean isMainComponent() { return isMainComponent; }
    public List<ReachMethodJson> getMethods() { return methods; }
}