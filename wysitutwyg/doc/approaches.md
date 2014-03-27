Let us examine possible approaches to the generation of the text. Here are our restrictions:

* It must be reasonably fast
* It must use Markov Chains to generate a segment-to-segment mapping of text beyond a direct one-to-one substitution of words

Wait, is that what is wanted? Hmm. Well, let's think about what the final product should be (funny that it's so far in and I haven't yet crystallized a good idea of "What I Want To Build" yet, but this whole thing was a lark off a xkcd comic strip sooooo)...

What is a WYSIWYG editor? WYSIWYG refers to usually layout and finished content, NOT content. So, a WYSIWYG editor allows the user to manipulate layout and markup directly on the page, where a WYSINWYG would not present the finalized layout but would produce it on command.

The joke in the strip is that the WYSITUTWYG has no content relationship to what is put in the editor.

So, what do we want the final product to be?

You type normally, but your output is nonsense sentences. The sentences are full sentences, though - they have the proper number of words (if you type "Hi, my name is Bob!" the sentence will have five words, and end with a sentence-ending punctuation mark). It should generate sentences from a selected corpus, with chains of n length (1, 2, or 3). You should also be able to save and load documents, either to your hard drive or the server.

Bonus points for the ability to add in italics, bolding, and other effects through use of key combos/buttons, which will be reflected in the output as applied to the proper words.

The output need not be immediate - it can take, say, half a second or so - but it should complete in a reasonable timeframe. The application itself should *not* have noticable keyboard delay on input.

# Sentence Generation

We want to generate a intelligible sentence of length equal to n from a specified corpus.

To do this, we'll use a Markov Chain, with start nodes being words which begin sentences, and end nodes being punctuation indicating the end of sentences. The main issue is how to generate a sentence of the designated length.

The current plan is to use a DFS to find the first randomly-chosen sentence which matches the prerequisite length.

# Displaying The Output

There are a few approaches I'm considering.

* Parse everything with Javascript on a time interval. This may end up being unresponsive and costly, in which case it might be better to...
* Parse everything (again with Javascript) on a per-change basis. That is, build a rich-text-style editor which can track changes and spans, allowing us to avoid having to reparse and recalculate the majority of the text.
* Send the text to the server, which returns the output to the client, after significant changes are made/the user pauses input for a specified timeframe.

Which of these I'll end up doing will probably depend on performance - I'll start with the naive approach and if it's unbearably costly for large corpora/texts, will optimize from there.