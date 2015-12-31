#import "LDFacebookPlugin.h"

static NSDictionary * errorToDic(NSError * error)
{
    return @{@"code":[NSNumber numberWithInteger:error.code], @"message":error.localizedDescription};
}
static NSDictionary * toError(NSString * message)
{
    return @{@"code":[NSNumber numberWithInteger:0], @"message":message};
}

static NSDictionary * sessionToDic(LDFacebookSession * session)
{
    NSMutableDictionary * dic = [NSMutableDictionary dictionary];
    [dic setObject:session.accessToken ?: @"" forKey:@"accessToken"];
    [dic setObject:[NSNumber numberWithInteger:session.expirationDate.timeIntervalSinceReferenceDate] forKey:@"expirationDate"];
    [dic setObject:[NSNumber numberWithInteger:session.state] forKey:@"state"];
    [dic setObject:session.user ?: @{} forKey:@"user"];
    [dic setObject:session.permissions ?: @[] forKey:@"permissions"];
    return dic;
}

@implementation LDFacebookPlugin
{
    CDVInvokedUrlCommand * _fbCallback;
    LDFacebookService * _service;
}

-(void) pluginInitialize
{
    [super pluginInitialize];
    if (!_service) {
        _service = [[LDFacebookService alloc] init];
    }
}

- (void)handleOpenURL:(NSNotification*)notification
{
    
}

- (void)dispose
{
    _service = nil;
    _fbCallback = nil;
}

-(void) setListener:(CDVInvokedUrlCommand*) command
{
    _fbCallback = command;
}


-(void) initialize:(CDVInvokedUrlCommand*) command
{
    _service.delegate = self;
    [_service initialize];
}

-(void) login:(CDVInvokedUrlCommand*) command
{
    NSDictionary * dic = [command argumentAtIndex:0 withDefault:@{} andClass:[NSDictionary class]];
    
    NSString * scope = [dic objectForKey:@"scope"];
    NSArray * permissions = nil;
    if (scope && [scope isKindOfClass:[NSString class]]) {
        permissions = [scope componentsSeparatedByString:@","];
    }
    
    [_service loginWithReadPermissions:permissions fromViewController:self.viewController completion:^(LDFacebookSession *session, NSError *error) {
        [self notify:command response:sessionToDic(session) error:error keep:NO];
    }];
}

-(void) logout:(CDVInvokedUrlCommand *) command
{
    [_service logout];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

-(void) requestAdditionalPermissions:(CDVInvokedUrlCommand *) command
{
    NSString * permissionType = [command argumentAtIndex:0 withDefault:@"read" andClass:[NSString class]];
    NSArray * permissions = [command argumentAtIndex:1 withDefault:@[] andClass:[NSArray class]];
    
    [_service requestAdditionalPermissions:permissionType permissions:permissions fromViewController:self.viewController completion:^(LDFacebookSession *session, NSError *error) {
        [self notify:command response:sessionToDic(session) error:error keep:NO];
    }];
    
}

-(void) api:(CDVInvokedUrlCommand *) command
{
    NSString * openGraph = [command argumentAtIndex:0 withDefault:@"" andClass:[NSString class]];
    NSString * method = [command argumentAtIndex:1 withDefault:@"GET" andClass:[NSString class]];
    NSDictionary * params = [command argumentAtIndex:2 withDefault:nil andClass:[NSDictionary class]];
    [_service api:openGraph method:method params:params completion:^(NSDictionary *response, NSError *error) {
        [self notify:command response:response error:error keep:NO];
    }];
}

-(void) ui:(CDVInvokedUrlCommand *) command
{
    NSDictionary * params = [command argumentAtIndex:0 withDefault:@{} andClass:[NSDictionary class]];
    NSString * methodName = [params objectForKey:@"method"];
    if (!methodName) {
        [self notify:command response:nil error:[NSError errorWithDomain:@"Facebook" code:0 userInfo:@{NSLocalizedDescriptionKey:@"Missing 'method' property in params/options array"}] keep:NO];
        return;
    }
    
    [_service ui:methodName params:params completion:^(NSDictionary *response, NSError *error) {
        [self notify:command response:response error:error keep:NO];
    }];
}

-(void) showShareDialog:(CDVInvokedUrlCommand *) command
{
    NSDictionary * params = [command argumentAtIndex:0 withDefault:@{} andClass:[NSDictionary class]];
    [_service showShareDialog:params completion:^(NSDictionary *response, NSError *error) {
        [self notify:command response:response error:error keep:NO];
    }];
}

-(void) uploadPhoto:(CDVInvokedUrlCommand *) command
{
    NSString * file = [command argumentAtIndex:0 withDefault:@"" andClass:[NSString class]];
    [_service uploadPhoto:file completion:^(NSDictionary *response, NSError *error) {
        [self notify:command response:response error:error keep:NO];
    }];
}


#pragma mark - LDFacebookServiceDelegate

-(void) facebookService:(LDFacebookService *) service didChangeLoginStatus:(LDFacebookSession *) session error:(NSError*) error
{
    if (_fbCallback) {
        [self notify:_fbCallback response:sessionToDic(session) error:error keep:YES];
    }
}

#pragma mark - Helpers

-(void) notify:(CDVInvokedUrlCommand *) command response:(NSDictionary *) response error:(NSError*) error keep:(BOOL) keep {
    NSMutableArray * arguments = [NSMutableArray array];
    [arguments addObject:response ?: @{}];
    if (error) {
        [arguments addObject:errorToDic(error)];
    }
    CDVPluginResult * result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsMultipart:arguments];
    if (keep) {
        [result setKeepCallbackAsBool:YES];
    }
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

@end
