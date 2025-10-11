package com.example.sandbox_backend.mappings;


import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class Language {

    final BiMap<String, String> languageMap = new ImmutableBiMap.Builder<String, String>()
            .put("c", ".c")
            .put("cpp", ".cpp")
            .put("javascript", ".js")
            .put("java", ".java")
            .put("rust", ".rs")
            .put("python", ".py")
            .put("go", ".go")
            .put("php", ".php")
            .put("csharp", ".cs")
            .build();

    final Map<String, String> execCommand = new HashMap<String, String>() {{
        put("c", "gcc App.c -o App && stdbuf -o0 ./App");
        put("cpp", "g++ App.cpp -o App && stdbuf -o0 ./App");
        put("javascript", "stdbuf -o0 node App.js");
        put("java", "javac App.java && stdbuf -o0 java App");
        put("rust", "rustc App.rs -o App && stdbuf -o0 ./App");
        put("python", "stdbuf -o0 python3 App.py");
        put("go", "go build -o App App.go && stdbuf -o0 ./App");
        put("php", "stdbuf -o0 php App.php");
        put("csharp", "mcs App.cs -out:App.exe && stdbuf -o0 mono App.exe");
    }};


    public boolean isLanguageValid(String language) {
        return languageMap.containsKey(language);
    }

    public BiMap<String, String> getLanguageMap() {
        return languageMap;
    }

    public Map<String, String> getExecCommand() {
        return execCommand;
    }
}