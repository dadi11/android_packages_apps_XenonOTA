/**
 * Copyright (C) 2018 XenonHD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by Devil7DK for XenonHD
 */

package com.xenonota.utils;

import java.io.File;
import java.io.FileWriter;

public class ORSUtils {
    private static String ors_path = "/cache/recovery/openrecoveryscript";

    private ORSUtils(){
    }
    public static void clear(){
        try{
            new File(ors_path).delete();
        }catch(Exception ex){ex.printStackTrace();}
    }

    public static void InstallZip(String absolute_path){
        WriteCommand("install " + absolute_path);
    }

    private static void WriteCommand(String command){
        try{
            FileWriter fw = new FileWriter(ors_path, true);
            fw.write(command + "\n");
            fw.close();
        }catch(Exception ex){ex.printStackTrace();}
    }

}
