;; This file removes trailing whitespace from source code; the trailing
;; whitespace serves no purpose.

(defun add-whitespace-hooks ()
  (add-hook 'write-contents-hooks 'maybe-remove-trailing-whitespace))

(add-hook 'c-mode-hook 'add-whitespace-hooks)
(add-hook 'java-mode-hook 'add-whitespace-hooks)

;; Customize the tests if desired.
(defun maybe-remove-trailing-whitespace ()
  "Remove trailing whitespace and newlines from all lines in the file,
unless the file is maintained by someone else."
  (if (not (and (buffer-file-name)
		(emacs-source-file-p (buffer-file-name))))
      (progn
	(remove-trailing-whitespace)
	(remove-trailing-newlines))))

(defun emacs-source-file-p (filename)
  "Return t if FILENAME is an Emacs source file."
  (or (string-match "mernst/emacs/x?lisp/" filename)
      (string-match "emacs[-/][0-9]+\.[0-9]+\\(\.[0-9]+\\)?/\\(lisp\\|src\\)/" filename)
      ;; (string-match "local/src/emacs-19" (buffer-file-name))
      ;; (string-match "lib/emacs/local-lisp/w3" (buffer-file-name)))
      ))

(defun remove-trailing-whitespace ()
  "Remove trailing whitespace from all lines in the file.  Does not modify point."
  (interactive)
  (save-excursion
    (goto-char (point-min))
    ;; Don't use this; in some modes (eg C mode), it removes blank lines.
    ;; Also, don't use replace-regexp, as it messages "Done."
    ;; (replace-regexp "\\s +$" "" nil)
    (while (re-search-forward "[\t ]+$" nil t)
      ;; Don't remove whitespace immediately following a comment starter.
      (let ((match-begin (match-beginning 0)))
	(if (or (= match-begin (point-min))
		(not (= ?< (char-syntax (char-after (1- match-begin))))))
	    (replace-match "")))))
  ;; Return nil so this can be used as a write-{file,contents}-hook
  nil)

(defun remove-trailing-newlines ()
  "Remove trailing blank lines from the file.  Does not modify point."
  (interactive)
  (goto-char (point-max))
  (if (re-search-backward "\\([^\n]\n\\)\n+\\'" nil t)
      (replace-match "\\1"))
  ;; Return nil so this can be used as a write-{file,contents}-hook
  nil)
