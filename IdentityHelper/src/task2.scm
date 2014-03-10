(define (displaytext tvSSH)
  (.setText tvSSH "This text came from another server. MAGIC!")
  (android.util.Log.e "CSP" "from jscheme"))

