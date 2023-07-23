package com.apkide.analysis.clm;

import com.apkide.analysis.clm.api.Entity;
import com.apkide.analysis.clm.api.FileEntry;
import com.apkide.analysis.clm.api.Member;
import com.apkide.analysis.clm.api.Type;

public interface ToolsAbstraction {
   boolean isHighlighted();

   void requestParameterList(FileEntry fileEntry, int line, int column);

   void requestCompletionList(FileEntry fileEntry, int line, int column);

   void requestClassesCompletionList(FileEntry fileEntry);

   void requestSuperMethodsCompletionList(FileEntry fileEntry, int line, int column);

   void documentize(FileEntry fileEntry, int line, int column);

   void documentize(FileEntry fileEntry);

   void surroundWithTryCatch(FileEntry fileEntry, int startLine, int startColumn, int endLine, int endColumn);

   void addThrows(FileEntry fileEntry, int line, int column);

   void safeDelete(Entity entity);

   void createVariable(FileEntry fileEntry, int line, int column, int identifier, Type type);

   void createField(FileEntry fileEntry, int line, int column, int modifiers, int identifier, Type type);

   void createMethod(FileEntry fileEntry, int line, int column, int modifiers, int identifier, Type type, int[] argModifiers, Type[] argTypes, int[] argNames);

   void inlineVariable(FileEntry fileEntry,int line, int column);

   void createSettersAndGetters(FileEntry fileEntry, int startLine, int startColumn, int endLine, int endColumn);

   void createConstructor(FileEntry fileEntry, int startLine, int startColumn, int endLine, int endColumn);

   void extractMethod(FileEntry fileEntry, int startLine, int startColumn, int endLine, int endColumn);

   void implementMembers(FileEntry fileEntry, int line, int column);

   void overrideMember(FileEntry fileEntry, int line, int column, Member member);

   void introduceVariable(FileEntry fileEntry, int startLine, int startColumn, int endLine, int endColumn);

}