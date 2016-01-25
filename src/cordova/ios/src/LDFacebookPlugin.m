#import "LDFacebookPlugin.h"
#import <Cordova/CDV.h>

static NSDictionary * errorToDic(NSError * error)
{
    return @{@"code":[NSNumber numberWithInteger:error.code], @"message":error.localizedDescription};
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

-(void) getLoginStatus:(CDVInvokedUrlCommand*) command
{
    NSNumber* force = [command argumentAtIndex:0 withDefault:0 andClass:[NSNumber class]];
   
    [_service getLoginStatus:[force boolValue] completion:^(LDFacebookSession *session, NSError *error) {
        [self notify:command response:sessionToDic(session) error:error keep:NO];
    }];
}

-(void) logout:(CDVInvokedUrlCommand *) command
{
    if ([_service isLoggedIn])
        [self notify:command response:nil error:nil keep:NO];
    
    [_service logout];
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



#import <objc/runtime.h>

#pragma mark SwizzledMethods

static NSArray* ClassGetSubclasses(Class parentClass)
{
    int numClasses = objc_getClassList(nil, 0);
    Class* classes = nil;
    
    classes = (Class*)malloc(sizeof(Class) * numClasses);
    numClasses = objc_getClassList(classes, numClasses);
    
    NSMutableArray* result = [NSMutableArray array];
    for (NSInteger i = 0; i < numClasses; i++) {
        
        Class superClass = classes[i];
        do {
            superClass = class_getSuperclass(superClass);
        }
        while(superClass && superClass != parentClass);
        
        if (superClass == nil) {
            continue;
        }
        
        [result addObject:classes[i]];
    }
    
    free(classes);
    
    return result;
}

static BOOL MethodSwizzle(Class clazz, SEL originalSelector, SEL overrideSelector)
{
    Method originalMethod = class_getInstanceMethod(clazz, originalSelector);
    Method overrideMethod = class_getInstanceMethod(clazz, overrideSelector);
    
    // try to add, if it does not exist, replace
    if (class_addMethod(clazz, originalSelector, method_getImplementation(overrideMethod), method_getTypeEncoding(overrideMethod))) {
        class_replaceMethod(clazz, overrideSelector, method_getImplementation(originalMethod), method_getTypeEncoding(originalMethod));
    }
    // add failed, so we exchange
    else {
        method_exchangeImplementations(originalMethod, overrideMethod);
        return YES;
    }
    
    return NO;
}

static Class ClassToSwizzle() {
    Class clazz = [CDVAppDelegate class];
    
    NSArray* subClazz = ClassGetSubclasses(clazz);
    if ([subClazz count] > 0) {
        clazz = [subClazz objectAtIndex:0];
    }
    
    return clazz;
}


#pragma mark Global Variables

static BOOL ldOpenURLExchanged = NO;


#pragma mark CDVAppDelegate (SwizzledMethods)

@implementation CDVAppDelegate (SwizzledMethods)

+ (void)load
{
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        Class clazz = ClassToSwizzle();
        ldOpenURLExchanged = MethodSwizzle(clazz, @selector(application:openURL:sourceApplication:annotation:), @selector(swizzle_application:openURL:sourceApplication:annotation:));
    });
    
}

-(BOOL)swizzle_application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation
{
    BOOL handled = [LDFacebookService application:application openURL:url sourceApplication:sourceApplication annotation:annotation];
    
    // if method was exchanged through method_exchangeImplementations, we call ourselves (no, it's not a recursion)
    if (!handled && ldOpenURLExchanged) {
        return [self swizzle_application:application openURL:url sourceApplication:sourceApplication annotation:annotation];
    }
    return NO;
}

@end
