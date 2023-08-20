package com.apkide.ls.xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apkide.ls.api.CodeCompiler;
import com.apkide.ls.api.CodeCompleter;
import com.apkide.ls.api.CodeFormatter;
import com.apkide.ls.api.CodeHighlighter;
import com.apkide.ls.api.CodeNavigation;
import com.apkide.ls.api.CodeRefactor;
import com.apkide.ls.api.LanguageServer;
import com.apkide.ls.api.Model;
import com.apkide.ls.api.highlighting.FileHighlighter;

public class XmlLanguageServer implements LanguageServer {
    private Model myModel;
    private XmlLexer myLexer = new XmlLexer();
    private FileHighlighter myHighlighter;
    
    @Override
    public void initialize(@NonNull Model model) {
        myModel = model;
        myHighlighter = new FileHighlighter(myModel, myLexer);
    }
    
    @Override
    public void shutdown() {
    
    }
    
    @Override
    public void configureRootPah(@NonNull String rootPath) {
    
    }
    
    @NonNull
    @Override
    public String getName() {
        return "XML";
    }
    
    @NonNull
    @Override
    public String[] getDefaultFilePatterns() {
        return new String[]{"*.xml"};
    }
    
    @Nullable
    @Override
    public CodeCompleter getCompleter() {
        return null;
    }
    
    @Nullable
    @Override
    public CodeFormatter getFormatter() {
        return null;
    }
    
    @Nullable
    @Override
    public CodeHighlighter getHighlighter() {
        return null;
    }
    
    @Nullable
    @Override
    public CodeNavigation getNavigation() {
        return null;
    }
    
    @Nullable
    @Override
    public CodeRefactor getRefactor() {
        return null;
    }
    
    @Nullable
    @Override
    public CodeCompiler getCompiler() {
        return null;
    }
    
}