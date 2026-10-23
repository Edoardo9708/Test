package it.edo.codecprobe;

import android.widget.TextView;

/** Do not request text measurement/layout for an unchanged value. */
final class UiText {
    static void set(TextView view,CharSequence text){if(!android.text.TextUtils.equals(view.getText(),text))view.setText(text);}
    private UiText(){}
}
