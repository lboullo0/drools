package org.drools.compiler.kie.builder.impl;

import org.drools.core.common.ProjectClassLoader;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.KieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.drools.core.common.ProjectClassLoader.createProjectClassLoader;
import static org.drools.core.rule.JavaDialectRuntimeData.convertResourceToClassName;

/**
 * Discovers all KieModules on the classpath, via the kmodule.xml file.
 * KieBaseModels and KieSessionModels are then indexed, with helper lookups
 * Each resulting KieModule is added to the KieRepository
 *
 */
public class KieModuleKieProject extends AbstractKieProject {

    private static final Logger                  log               = LoggerFactory.getLogger( KieModuleKieProject.class );

    private Map<ReleaseId, InternalKieModule>          kieModules;

    private final Map<String, InternalKieModule> kJarFromKBaseName = new HashMap<String, InternalKieModule>();

    private final InternalKieModule              kieModule;
    private final KieRepository                  kr;
    private ProjectClassLoader cl;

    public KieModuleKieProject(InternalKieModule kieModule,
                               KieRepository kr) {
        this.kieModule = kieModule;
        this.kr = kr;
        this.cl = createProjectClassLoader();
    }

    public void init() {
        if ( kieModules == null ) {
            kieModules = new HashMap<ReleaseId, InternalKieModule>();
            kieModules.putAll( kieModule.getDependencies() );
            kieModules.put( kieModule.getReleaseId(),
                            kieModule );
            indexParts( kieModules, kJarFromKBaseName );
            initClassLoader();
        }
    }

    private void initClassLoader() {
        for (Map.Entry<String, byte[]> entry : getClassesMap().entrySet()) {
            if (entry.getValue() != null) {
                String resourceName = entry.getKey();
                String className = convertResourceToClassName(resourceName);
                cl.storeClass(className, resourceName, entry.getValue());
            }
        }
    }

    private Map<String, byte[]> getClassesMap() {
        Map<String, byte[]> classes = new HashMap<String, byte[]>();
        for ( InternalKieModule kModule : kieModules.values() ) {
            // avoid to take type declarations defined directly in this kieModule since they have to be recompiled
            classes.putAll(kModule.getClassesMap(kModule != this.kieModule));
        }
        return classes;
    }

    public ReleaseId getGAV() {
        return kieModule.getReleaseId();
    }

    public InternalKieModule getKieModuleForKBase(String kBaseName) {
        return this.kJarFromKBaseName.get(kBaseName);
    }

    public boolean kieBaseExists(String kBaseName) {
        return kBaseModels.containsKey(kBaseName);
    }

    @Override
    public ClassLoader getClassLoader() {
        return this.cl;
    }

}