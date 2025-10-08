/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Singleton.java to edit this template
 */
package edu.utmb.ontology.hsrb.kgmanager;

import com.github.jsonldjava.shaded.com.google.common.base.Charsets;
import com.github.jsonldjava.shaded.com.google.common.base.Strings;
import com.github.jsonldjava.shaded.com.google.common.io.CharSink;
import com.github.jsonldjava.shaded.com.google.common.io.FileWriteMode;
import com.github.jsonldjava.shaded.com.google.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

import org.semanticweb.owlapi.vocab.OWL2Datatype;


/**
 *
 * @author mac
 */
public class HSRBController {
    
    final private String kg_file = "/Users/mac/Documents/GitHub/NDKG/ndkg-base.owl";
    final private String namespace = "http://purl.org/utmb/ndkg-base.owl";
    
    private OWLOntology ontology = null;
    private OWLOntologyManager manager = null;
    private OWLDataFactory factory = null;
    
    private OWLAnnotation notes = null;
    
    private OWLEntityRenamer renamer;
    
    private AtomicLong id_counter = new AtomicLong(0);
    
    private OWLClass parentClassFolder;
    
    private HSRBController() {
        this.initialize();

        parentClassFolder = factory.getOWLClass(namespace + "#_NDKG");
    }
    
    public void organizeClassesUnderParent(){
        
        ontology.classesInSignature().forEach(oc->{
            
            OWLSubClassOfAxiom org = factory.getOWLSubClassOfAxiom(oc, parentClassFolder);
            
            ontology.add(org);
            
        });
        
    }
    
    public void getMaximumNumberID(){
        
        ontology.signature().forEach(entity->{
            
            if(entity.isOWLClass()){
                
                if(entity.getIRI().toString().contains("#KGN_")){
                    
                   
                    
                    long v = Long.parseLong(entity.getIRI().toString().
                            substring(
                            entity.getIRI().toString().indexOf("#KGN_")+5,
                            entity.getIRI().toString().length()));
                    
                    
                    if(id_counter.get() < v){
                        id_counter.set(v);
                    }

                    
                }
                
            }
            
            if(entity.isOWLObjectProperty()){
                
            }
            
        });
        
        System.out.println("largest id number is " + id_counter.get());
        
        long new_value = id_counter.incrementAndGet();
        
        System.out.println(" new value : " + new_value);
        
        id_counter.set(new_value);
        
        
    }
    
    
    private void initialize(){
        
        try {
            File ontology_file = new File(kg_file);
            
            manager = OWLManager.createConcurrentOWLOntologyManager();
            
            ontology = manager.loadOntologyFromOntologyDocument(ontology_file);
            
            factory = ontology.getOWLOntologyManager().getOWLDataFactory();
            
        } catch (OWLOntologyCreationException ex) {
            System.getLogger(HSRBController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        
        
        
    }
    
    private void recordOldReference(OWLClass class_reference){
        
        String old_iri_string = class_reference.getIRI().toString();
        
        OWLAnnotationProperty change_note = factory.getOWLAnnotationProperty(IRI.create(SKOS.NOTE.stringValue()));
        OWLAnnotation change_note_annotation = factory.getOWLAnnotation(change_note, factory.getOWLLiteral(old_iri_string, "en"));
        OWLAnnotationAssertionAxiom change_note_assertion = factory.getOWLAnnotationAssertionAxiom(class_reference.getIRI(), change_note_annotation);

        this.manager.applyChange(new AddAxiom(ontology, change_note_assertion));
        
    }
    
    public void remapAllIRIs(){
        
        renamer = new OWLEntityRenamer(manager, Collections.singleton(ontology));
        
        Map<OWLEntity, IRI> iri_mapper = new HashMap<>();

        Set<OWLClass> all_classes = ontology.classesInSignature().collect(Collectors.toSet());
        
        all_classes.forEach(oc->{
            
           
           IRI iri = oc.getIRI();
           
           if(iri.toString().contains("#KGN_")){
               
               iri_mapper.put(oc, IRI.create(iri.toString().replaceFirst("#KGN_", "#NDKG_")));
               
           }
           else{
               
               
               
               iri_mapper.put(oc, IRI.create(namespace + "#NDKG_" + getNextIDValue()));
           }
           
            
        });
        
        ontology.applyChanges(renamer.changeIRI(iri_mapper));
        
        
        
    }
    
    private String getNextIDValue(){
        return Strings.padStart(String.valueOf(id_counter.getAndIncrement()), 7, '0');
    }
    
    public void recordAllOldReferences(){
        
        Set<OWLClass> all_classes = ontology.classesInSignature().collect(Collectors.toSet());
        
        all_classes.forEach(c->this.recordOldReference(c));

    }
    
    
    
    public void exportAllClassesToFile(String file_name){
        
        Set<OWLClass> all_classes = ontology.classesInSignature().collect(Collectors.toSet());
        
        File outputFile = new File(file_name);
        CharSink charsink = Files.asCharSink(outputFile, Charsets.UTF_8);
        
        OWLAnnotationProperty definition_property = factory.getOWLAnnotationProperty(IRI.create("http://purl.org/utmb/ndkg.owl#definition"));
        
        StringBuilder content = new StringBuilder();
        
        content.append("IRI \t Label \t Definition" );
        
        all_classes.forEach(oe->{
            
            IRI iri = oe.asOWLClass().getIRI();
            
            content.append("\n"+iri );
            

                    Stream<OWLAnnotation> labels =EntitySearcher.getAnnotations(oe, ontology, factory.getRDFSLabel());
                    
                    labels.forEach(oa->{
                        
                        oa.getValue().asLiteral().ifPresent(ol->{
                            
                            //System.out.println("\t" + ol.getLiteral());
                            content.append("\t" +ol.getLiteral() );
                            
                        });
                        
                    
                    });
                    
                    Stream <OWLAnnotation> definition = EntitySearcher.getAnnotations(oe, ontology, definition_property);
                    
                    definition.forEach(def->{
                        
                     
                        Optional<OWLLiteral> l = def.getValue().asLiteral();
                        
                        l.ifPresent(a->{
                            if (a.hasLang("en")) content.append( "\t" +a.getLiteral().replaceAll("\t", " ").replaceAll("\n", " ") );
                            
                        });
                        
                        
                  
                    
                    
                    
                    });
                    
                    
            
        });
        
        try {
            charsink.write(content.toString());
        } catch (IOException ex) {
            System.getLogger(HSRBController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        
        
    }
    
    
    public void saveOntology(String file_name){
        try {
            
            manager.saveOntology(ontology, new OWLXMLDocumentFormat(), new FileOutputStream(new File(file_name)));
        
        } catch (FileNotFoundException ex) {
            System.getLogger(HSRBController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } catch (OWLOntologyStorageException ex) {
            System.getLogger(HSRBController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
    
    public static HSRBController getInstance() {
        return HSRBControllerHolder.INSTANCE;
    }
    
    private static class HSRBControllerHolder {

        private static final HSRBController INSTANCE = new HSRBController();
    }
    
    public static void main(String[] args) {
        
        HSRBController controller = HSRBController.getInstance();
        controller.getMaximumNumberID();
        controller.recordAllOldReferences();
        controller.remapAllIRIs();
        controller.organizeClassesUnderParent();
        controller.saveOntology("example.owl");
        
        //controller.exportAllClassesToFile("finalized.txt");
    }
}
