# pag-daikon.bashrc
# This file should be kept in sync with pag-daikon.cshrc.

export LC_ALL=${LC_ALL:-en_US}

export DAIKONPARENT=${DAIKONPARENT:-${HOME}/research}
export DAIKONDIR=${DAIKONPARENT}/invariants

if [ ! -d "${DAIKONDIR}" ] then
  echo "*****"
  echo "pag-daikon.bashrc cannot find ${DAIKONDIR}"
  echo "Please check out Daikon to correct this problem."
  echo "*****"
  # Default to Michael Ernst's version of Daikon, just so references to
  # ${INV} don't die, preventing this script from completing.  This is not
  # tested.
  if [ -d /afs/csail.mit.edu/u/m/mernst/research/invariants ] then
    export DAIKONDIR=/afs/csail.mit.edu/u/m/mernst/research/invariants
  fi
fi

export DAIKONBIN=${DAIKONDIR}/scripts
export INV=${DAIKONDIR}
export inv=${INV}
export DAIKONCLASS_SOURCES=1
export PAG=/afs/csail.mit.edu/group/pag
export pag=${PAG}

## Set this directory to the directory containing the JDK.
export JDKDIR=${JDKDIR:-/afs/csail/group/pag/software/pkg/jdk}

export PATH=/usr/local/bin:${PATH}:/afs/csail/group/pag/projects/invariants/binaries:$DAIKONDIR/front-end/c
export PATH=`echo $PATH | ${INV}/scripts/path-remove.pl`

source ${INV}/scripts/daikon.bashrc

export LD_LIBRARY_PATH=/usr/X11R6/lib:/usr/local/lib:/usr/lib:/lib

if [ -z "$DAIKON_LIBS" ]; then
  export DAIKON_LIBS=`/usr/bin/perl -e 'print join(":", @ARGV);' ${INV}/java/lib/*.jar`
  export CLASSPATH=.:${CLASSPATH}:${DAIKON_LIBS}
fi
export LACKWIT_HOME=${INV}/front-end/c/lackwit

# Remove duplicates so path and classpath don't get too long
export CLASSPATH=`echo $CLASSPATH | path-remove.pl`
export PATH=`echo $PATH | ${INV}/scripts/path-remove.pl`

## Someone needs to rewrite this as a shell function, since bash aliases
## can't handle arguments.
## # Like "cvs update", but filters out output that is unlikely to be of interest.
## # Alternately, run CVS under emacs via "M-x cvs-update".
## alias	cvsupdate	'cvs -q update -d \!* |& egrep -e "^C |update aborted|non-existent repository|Permission denied|cannot open|^cvs update: [^U]"'

export DFEJ_VERBOSE=1

# Enable use of group bibliographies, and the "bibfind" command.
alias bibfind='/afs/csail.mit.edu/u/m/mernst/bin/Linux-i686/help .n .F /afs/csail.mit.edu/u/m/mernst/bib/bibroot.non-mde'
export BIBINPUTS=.:/afs/csail.mit.edu/u/m/mernst/bib:..:

export EDITOR=${EDITOR:-emacsclient}
export ALTERNATE_EDITOR=${ALTERNATE_EDITOR:-emacs}
export VISUAL=${VISUAL:-emacsclient}
