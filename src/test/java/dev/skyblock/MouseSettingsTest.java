package dev.skyblock;

import dev.skyblock.game.MouseSettings;
import dev.skyblock.persistence.MouseSettingsStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class MouseSettingsTest {
    @TempDir Path directory;
    @Test void firstSessionDefaultsAreNonInvertedAndDoNotCreateAFile() throws Exception {
        Path p=directory.resolve("controls.properties"); assertEquals(MouseSettings.DEFAULT,MouseSettingsStore.load(p).value()); assertFalse(Files.exists(p));
    }
    @Test void axesAndSensitivitySurviveRestartIndependentlyOfWorldSaves() throws Exception {
        Path p=directory.resolve("config/controls.properties"); MouseSettingsStore store=MouseSettingsStore.load(p);
        MouseSettings next=new MouseSettings(true,false,.275); store.save(next); assertEquals(next,store.value()); assertEquals(next,MouseSettingsStore.load(p).value());
        next=new MouseSettings(false,true,.4); store.save(next); assertEquals(next,MouseSettingsStore.load(p).value());
    }
    @Test void missingOrInvalidFieldsUseDefaultsWithoutLosingValidAxes() throws Exception {
        Path p=directory.resolve("controls.properties");
        for(String invalid:new String[]{"NaN","Infinity","-1","0","1.1","broken"}) {
            Files.writeString(p,"mouse.invertX=true\nmouse.invertY=false\nmouse.sensitivity="+invalid+"\n");
            MouseSettings s=MouseSettingsStore.load(p).value(); assertTrue(s.invertX()); assertFalse(s.invertY()); assertEquals(.15,s.sensitivity());
        }
        Files.writeString(p,"mouse.invertY=true\n"); assertEquals(new MouseSettings(false,true,.15),MouseSettingsStore.load(p).value());
    }
    @Test void failedSaveDoesNotApplyAnUnpersistedConfiguration() throws Exception {
        Path parent=directory.resolve("blocked"); Path path=parent.resolve("controls.properties"); MouseSettingsStore store=MouseSettingsStore.load(path);
        Files.writeString(parent,"A file blocks creation of the configuration directory");
        assertThrows(IOException.class,()->store.save(MouseSettings.DEFAULT.toggleX())); assertEquals(MouseSettings.DEFAULT,store.value());
    }
    @Test void repeatedSavesAndResetLeaveOnlyTheSettingsFile() throws Exception {
        Path path=directory.resolve("controls.properties"); MouseSettingsStore store=MouseSettingsStore.load(path);
        store.save(new MouseSettings(true,true,.5)); store.save(MouseSettings.DEFAULT);
        assertEquals(MouseSettings.DEFAULT,MouseSettingsStore.load(path).value());
        try(var files=Files.list(directory)) { assertEquals(1,files.count()); }
    }
    @Test void sensitivityIsFiniteAndBounded() {
        for(double n:new double[]{Double.NaN,Double.POSITIVE_INFINITY,0,-1,1.1}) assertThrows(IllegalArgumentException.class,()->new MouseSettings(false,false,n));
        assertEquals(.01,new MouseSettings(false,false,.02).adjustSensitivity(-.025).sensitivity());
        assertEquals(1,new MouseSettings(false,false,.99).adjustSensitivity(.025).sensitivity());
    }
}
