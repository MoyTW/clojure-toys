# Summary

The WYSITUTWYG editor is an online text editor whose output text is completely semantically unrelated to the input text, but which has the same rough form and shares markup text with the original form. For example, if you put the following text into the editor:

"My Grandpa fought in China during the Second World War."

you might see in the output

"But who among us would choose to take pain voluntarily?"

# Text Generation

The text is generated on the server by means of a Markov Model, built from a selectable corpus. A Clojurescript update function will request the full conversion from the server during lulls in user typing.

# Editor Elements

The user should be able to:

* Select the corpus from which the random text is generated
* Select the chaining length used in the text generation
* Bold or italicize text through the UI, and have it show up in the output