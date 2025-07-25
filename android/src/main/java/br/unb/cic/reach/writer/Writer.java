package br.unb.cic.reach.writer;

import java.io.File;
import java.util.Set;

import br.unb.cic.reach.model.ReachClass;

public interface Writer {

    void write(Set<ReachClass> result, File out);

}
