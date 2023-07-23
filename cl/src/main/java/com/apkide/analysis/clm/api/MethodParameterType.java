package com.apkide.analysis.clm.api;

import static com.apkide.analysis.clm.api.TypeSemantic.UNKNOWN_SEMANTIC;

import com.apkide.analysis.clm.api.collections.ListOf;
import com.apkide.analysis.clm.api.collections.StoreInputStream;
import com.apkide.analysis.clm.api.collections.StoreOutputStream;
import com.apkide.analysis.clm.api.excpetions.UnknownEntityException;

import java.io.IOException;

public final class MethodParameterType extends Type {
   private int myID;
   private final EntitySpace mySpace;
   private final IdentifierSpace myIdentifiers;
   private final FileSpace myFileSpace;
   private FileEntry myFile;
   private int myDeclarationNumber;
   private int myLine;
   private int myStartColumn;
   private int myEndColumn;
   private int myIdentifier;
   private Member myMethod;
   private int myNumber;
   private ListOf<Type> myBoundTypes;
   private boolean myTypesLoaded;

   protected MethodParameterType(EntitySpace space, IdentifierSpace identifiers, FileSpace fileSpace) {
      super(space, UNKNOWN_SEMANTIC);
      this.mySpace = space;
      this.myIdentifiers = identifiers;
      this.myFileSpace = fileSpace;
   }

   protected MethodParameterType(EntitySpace space, IdentifierSpace identifiers, FileSpace filespace, FileEntry file, int declarationNumber) {
      super(space, UNKNOWN_SEMANTIC);
      this.mySpace = space;
      this.myIdentifiers = identifiers;
      this.myFileSpace = filespace;
      this.myID = space.declareEntity(this);
      this.myFile = file;
      this.myDeclarationNumber = declarationNumber;
      this.myBoundTypes = null;
      this.myTypesLoaded = false;
   }

   @Override
   protected void load(StoreInputStream stream) throws IOException {
      super.load(stream);
      this.myLine = stream.readInt();
      this.myStartColumn = stream.readInt();
      this.myEndColumn = stream.readInt();
      this.myID = stream.readInt();
      this.myFile = this.myFileSpace.getFileEntry(stream.readInt());
      this.myDeclarationNumber = stream.readInt();
      this.myIdentifier = stream.readInt();
      this.myMethod = (Member)this.mySpace.getEntity(stream.readInt());
      this.myNumber = stream.readInt();
      this.myTypesLoaded = stream.readBoolean();
      if (stream.readBoolean()) {
         this.myBoundTypes = new ListOf<>(this.mySpace, stream);
      }
   }

   @Override
   protected void store(StoreOutputStream stream) throws IOException {
      super.store(stream);
      stream.writeInt(this.myLine);
      stream.writeInt(this.myStartColumn);
      stream.writeInt(this.myEndColumn);
      stream.writeInt(this.myID);
      stream.writeInt(this.myFile.getID());
      stream.writeInt(this.myDeclarationNumber);
      stream.writeInt(this.myIdentifier);
      stream.writeInt(this.mySpace.getID(this.myMethod));
      stream.writeInt(this.myNumber);
      stream.writeBoolean(this.myTypesLoaded);
      stream.writeBoolean(this.myBoundTypes != null);
      if (this.myBoundTypes != null) {
         this.myBoundTypes.store(stream);
      }
   }

   protected void invalidate() {
      if (this.myBoundTypes != null) {
         this.myBoundTypes.clear();
      }

      this.myTypesLoaded = false;
   }

   protected void declareBoundType(Type boundType) {
      this.myBoundTypes.add(boundType);
      this.myTypesLoaded = true;
   }

   protected void declarePositions(int line, int startColumn, int endColumn) {
      this.myLine = line;
      this.myStartColumn = startColumn;
      this.myEndColumn = endColumn;
   }

   protected void declareSyntax(int identifier, Member method, int number) {
      this.myIdentifier = identifier;
      this.myMethod = method;
      this.myNumber = number;
      if (this.myBoundTypes == null) {
         this.myBoundTypes = new ListOf<>(this.mySpace);
      } else {
         this.myBoundTypes.clear();
      }
   }

   @Override
   public int getIdentifierLine() {
      this.mySpace.loadPositions(this.myFile);
      return this.myLine;
   }

   @Override
   public int getIdentifierEndColumn() {
      this.mySpace.loadPositions(this.myFile);
      return this.myEndColumn;
   }

   @Override
   public int getIdentifierStartColumn() {
      this.mySpace.loadPositions(this.myFile);
      return this.myStartColumn;
   }

   public Member getMethod() {
      return this.myMethod;
   }

   public int getNumber() {
      return this.myNumber;
   }

   @Override
   public FileEntry getFile() {
      return this.myFile;
   }

   @Override
   public int getDeclarationNumber() {
      return this.myDeclarationNumber;
   }

   public int getBoundTypeCount() {
      this.loadBounds();
      return this.myBoundTypes != null && this.myBoundTypes.size() != 0 ? this.myBoundTypes.size() : 1;
   }

   public Type getBoundType(Type type, int number) throws UnknownEntityException {
      Type membertype = this.getBoundType(number);
      if (type.isClassType()) {
         Type containingtype = ((ClassType)type).getContainingTypeOfMember(this.myMethod);
         return containingtype.isParameterizedType() ? ((ParameterizedType)containingtype).replaceType(membertype) : membertype;
      } else {
         Type containingtype = ((ParameterizedType)type).getClassType().getContainingTypeOfMember(this.myMethod);
         return !containingtype.isParameterizedType()
            ? ((ParameterizedType)type).replaceType(membertype)
            : ((ParameterizedType)type).replaceType(((ParameterizedType)containingtype).replaceType(membertype));
      }
   }

   public Type getBoundType(int number) throws UnknownEntityException {
      this.loadBounds();
      return this.myBoundTypes != null && this.myBoundTypes.size() > 0 ? this.myBoundTypes.get(number) : this.mySpace.getRootClasstype(this.myFile);
   }

   @Override
   public Type getErasedType() throws UnknownEntityException {
      this.loadBounds();
      return this.myBoundTypes != null && this.myBoundTypes.size() > 0 ? this.myBoundTypes.get(0) : this.mySpace.getRootClasstype(this.myFile);
   }

   @Override
   public ClassType getEnclosingClassType() {
      return this.myMethod.getEnclosingClassType();
   }

   @Override
   public ClassType getEnclosingTopLevelClassType() {
      return this.myMethod.getEnclosingClassType().getEnclosingTopLevelClassType();
   }

   @Override
   public int getIdentifier() {
      return this.myIdentifier;
   }

   @Override
   public int getID() {
      return this.myID;
   }

   private void loadBounds() {
      if (!this.myTypesLoaded) {
         this.myTypesLoaded = true;
         if (this.myFile.getLanguage().getTypeSystem().inheritsBoundTypes() && this.myMethod.getOverriddenMembers().size() > 0) {
            try {
               Member oMethod = this.myMethod.getOverriddenMembers().get();
               int count = oMethod.getMethodParametertypeCount();

               for(int number = 0; number < count; ++number) {
                  MethodParameterType methodParameterType = oMethod.getMethodParameterType(number);
                  int identifier = methodParameterType.getIdentifier();
                  MethodParameterType methodparametertype = this.myMethod.accessParameterType(identifier);
                  int boundCount = methodParameterType.getBoundTypeCount();

                  for(int j = 0; j < boundCount; ++j) {
                     Type boundtype = methodParameterType.getBoundType(this.myMethod.getEnclosingClassType().parameterizeCanonical(), j);
                     this.mySpace.declareBoundtype(methodparametertype, boundtype);
                  }
               }
            } catch (UnknownEntityException ignored) {
            }
         } else {
            this.mySpace.loadTypes(this.myFile);
         }
      }
   }
}