# Separate audio/video workspaces (v0.61)

MainActivity remains the video workspace and the existing launcher/notification
entry point. AudioActivity is a separate, non-exported workspace. The fixed bottom
navigation opens Audio without finishing Video. Returning to Video reorders its
existing Activity to the front and finishes Audio; its audio draft is saved on
pause. There is at most one AudioActivity in normal navigation. Android task/back
behavior and notification entry continue to target MainActivity. Navigation does
not issue conversion commands or modify a saved video session.

Shared views, independent actions:
- AppUi: palette, system bars, buttons, sliders, page and insets. Its optional
  navigation footer stays outside the scroll view; legacy page-only callers keep
  their existing layout. Insets apply to the whole shell when a footer is present.
- AppHeader and AppNavigation: common header and Video/Audio navigation.
- AppSections: reusable accordion drawing/expansion. HomeSections supplies only
  video labels and the existing active-conversion initial focus rule. Audio has
  its own descriptions and independent expansion state.
- AudioScreen: audio-specific view composition with the same visual components.
- AudioController/AudioState: audio lifecycle and editable draft.
- AudioSelection: SAF audio multi-selection, persistent read grants, destination
  tree selection and serial lightweight filename queries. No broad audio/gallery
  permission is required. Unsupported persistent grants are explicitly reported.
- AudioDraftStore: only `audio-workspace` SharedPreferences. Selected URI/name
  pairs, folder, bitrate and expansion survive normal switching/recreation.
  A stored URI does not manufacture permissions; temporary grants may require
  reselection after process death. This is a draft, not conversion recovery.

Audio draft actions currently work: select multiple audio files, view filenames,
clear the audio selection, choose a writable/persistently granted folder, and
choose the planned Opus VBR bitrate (64–256 in steps of 8; initial 128 kbit/s).
The audio settings button opens the audio settings card. Audio encoding is NOT
implemented or presented as available. FLAC decoding, Opus encoding, metadata and
cover preservation, .lrc association, queue/report/log and audio recovery remain
subsequent work. There is no audio service, encoder or extra conversion worker.

Video parameters, saved queue, outputs/report, file/folder choices and logs retain
their existing owners. Audio never reads or writes AppSettings, SavedSession,
BatchRetry, HomeState or ConversionService. Its filename executor is owned by
AudioSelection, closes with the Activity and uses generation checks so late
results cannot repopulate a cleared/replaced selection. It does not decode media.
Before adding actual audio encoding, a common execution arbiter must cover both
engines so only one heavy conversion can run at a time. No cross-workspace queue
execution contract is claimed in this initial separation.

Tests cover shared view behavior, separate persisted audio state/expansion, the
video journal remaining byte-identical, multi-pick/deduplication/cancellation,
navigation Intents and retained video selection, and invalidation of late filename
callbacks. Existing video lifecycle and UI tests also run. Native code is unchanged;
the manifest registration can trigger the CI's conservative full-build scope.
