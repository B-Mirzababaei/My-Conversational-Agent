/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package edu.cmu.cs.lti.basilica2.generics;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

/**
 *
 * @author rohitk
 */
public class Memory<T> extends Component {

    private List<T> memoryElements;

    public Memory(Agent a, String n, String pf) {
        super(a, n, pf);
        memoryElements = new ArrayList<T>();
    }

    public void commit(T o) {
        memoryElements.add(0, o);
        this.informObservers("<commit type=\"" + o.getClass().getName() + "\">" + o.toString() + "</commit>");
    }

    public T retrieve() {
        if (memoryElements.size() > 0) {
            T o = memoryElements.get(0);
            this.informObservers("<retrieve position=\"0\">" + o.toString() + "</retrieve>");
            return o;
        }
        this.informObservers("<retrievalFailure/>");
        return null;
    }

    public T retrieve(int i) {
        T o = memoryElements.get(i);
        this.informObservers("<retrieve position=\"" + i + "\">" + o.toString() + "</retrieve>");
        return o;
    }

    public void clear() {
        this.informObservers("<memorycleared/>");
        memoryElements.clear();
    }

    public void remove() {
        if (memoryElements.size() > 0) {
            this.informObservers("<removed position=\"0\"/>");
            memoryElements.remove(0);
        }
        this.informObservers("<removalFailure/>");
    }

    public int historyLength() {
        return memoryElements.size();
    }

    protected void doMemoryDump(String f) {
        try {
            FileWriter fw = new FileWriter(f, true);
            fw.write("<memorydump date=\"" + Logger.getTimeStamp(true) + "\">\n");
            for (int i = 0; i < memoryElements.size(); i++) {
                fw.write(memoryElements.get(i).toString());
            }
            fw.write("\n</memorydump>");
            log(Logger.LOG_NORMAL, "Dumped Memory to File: " + f);
            fw.flush();
            fw.close();
        } catch (Exception e) {
            log(Logger.LOG_ERROR, "Error while updating Status File (" + e.toString() + ")");
        }
    }

    @Override
    public String getType() {
        return "Memory";
    }

    @Override
    protected void processEvent(Event e) {
    }
}
