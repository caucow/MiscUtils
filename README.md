# MiscUtils
MiscUtils is a small project containing various utility classes I've created and used in my other projects (open and closed source).

- ByteBufferChain
  - Byte buffer that can grow or shrink by adding/removing byte arrays to/from the beginning/end of the buffer
  - Meant to be for more general use, including get and getlast methods for all primitive types
  - Should be thread safe (my use case has the buffer shared by 2-3 threads, if you find a problem please create an issue report or PR)
  - Currently only has methods for reading, might add write methods later(tm)
  
- BufferChain
  - Identical to ByteBufferChain, except for use with a generic object type rather than bytes
    - This of course means only one get and getlast method
    
- JsonConfig
  - Requires GSON, though could probably be modified to work with a different JSON library
  - Originally used for loading a JSON configuration, the name stuck.
    - If the JsonConfig is kept persistent throughout the runtime of a program, the configuration it stores becomes forward-compatible (unless different versions of your program expect different values for the same key)
      - Literally just firing shots at Mojang here. options.txt is bad, plsfix. I know this will never be seen by anyone that can change that.
  - Meant to make accessing properties buried deep in the JSON easier (and similar to script languages), supporting a very limited dot notation as well as bracket notation with quotes for map/dictionary/whateveryouwanttocallthem access or numbers for array access.
    - ex `json.getString("keyInRoot.subkey[\"sub-sub-key with non-java-identifier chars\"][42]", null)`
