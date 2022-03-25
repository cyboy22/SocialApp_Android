enum class SlidanetRequestType { Connect,
                                 ConnectContent,
                                 DisconnectContent,
                                 DisconnectAllContent,
                                 Disconnect,
                                 None
}

internal enum class ShaderType { DefaultShader
}

internal enum class ShareModeType { Slide,
    Peek,
    Pix,
    MaxValue}

internal enum class SlideModeType { SlideNone,
    SlideDefinition,
    MaxValue }

internal enum class PeekModeType { PeekNone,
    PeekDefinition,
    PeekSlide,
    MaxValue}

internal enum class PixModeType { PixNone,
    PixDefinition,
    PixSlide,
    PixDynamic,
    MaxValue }

internal enum class VisibilityPreferenceType { RequestLess,
    Neutral,
    RequestMore}

internal enum class SlidanetMessageType { AuthenticateConnectionRequest,
                                          AuthenticateConnectionResponse,
    ConnectContentRequest,
    ConnectContentResponse,
    UpdateContentContextResponse,
    MoveContentRequest,
    MoveContentResponse,
    SetContentVisibilityPreferenceRequest,
    SetContentVisibilityPreferenceResponse,
    GiveContentRequest,
    GiveContentResponse,
    TakeContentRequest,
    TakeContentResponse,
    MoreContentRequest,
    MoreContentResponse,
    LessContentRequest,
    LessContentResponse,
    SetContentShareModeRequest,
    SetContentShareModeResponse,
    SetContentFilterRequest,
    SetContentFilterResponse,
    DisconnectContentRequest,
    DisconnectContentResponse,
    DisconnectAllContentRequest,
    DisconnectAllContentResponse,
    TakeNotificationRequest,
    VisibilityNotificationRequest,
    SetMuteContentRequest,
    SetMuteContentResponse,
    SetHideContentRequest,
    SetHideContentResponse,
    SetFreezeContentRequest,
    SetFreezeContentResponse,
    DisconnectRequest,
    DisconnectResponse,
    MaxValue }

enum class SlidanetResponseType { Ok,
                                  Undefined,
                                  RequestSubmitted,
                                  ApplicationNameContainsNonASCIICharacters,
                                  ApplicationNameTooLong,
                                  ApplicationPasswordTooLong,
                                  SlidaNameTooLong,
                                  ApplicationPasswordContainsNonASCIICharacters,
                                  ApplicationPasswordLengthGreaterThan32,
                                  SlidaNameContainsNonASCIICharacters,
                                  SlidaNameLengthGreaterThan32,
                                  BadAppContentPath,
                                  InvalidIpAddressFormat,
                                  InvalidPortNumber,
                                  ConnectionAuthenticated,
                                  DisconnectedFromAllContent,
                                  AppContentFileNotFound,
                                  UnableToDecodeAppContentFile,
                                  UnableToLoadInitialVideoFrame,
                                  NotConnectedToSlidanet,
                                  InvalidSlidanetContentAddress,
                                  InvalidAppVideoStartTime,
                                  AppContentConnectedToSlidanetAddress,
                                  SlidanetContentAddressNotFound,
                                  InvalidSlidanetContentAddressForPlatform,
                                  Disconnected,
                                  InternalErrorOccurred,
                                  OutstandingRequestExists,
                                  UnsupportedContentType,
                                  InvalidApplicationPassword,
                                  AuthenticationFailed,
                                  ConnectedToContent,
                                  DisconnectedFromContent,
                                  ContentAddressNotInApplication,
                                  ContentAddressNotFound,
                                  ViewGiven,
                                  ViewTaken,
                                  VisibilityPreferenceSet,
                                  ShareModeDefinitionSet,
                                  MuteSet,
                                  MuteUnset,
                                  HideSet,
                                  HideUnset,
                                  FreezeSet,
                                  FreezeUnset,
                                  MaxValue }


enum class SlidanetContentType { KImage, KVideo }

enum class SlidanetFilterType { Default }