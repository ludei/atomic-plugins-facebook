#import <Foundation/Foundation.h>


typedef NS_ENUM(NSInteger, LDFacebookSessionState) {
    LDFacebookSessionStateOpen = 0,
    LDFacebookSessionStateNotAuthorized,
    LDFacebookSessionStateClosed,
};

@interface LDFacebookSession: NSObject

@property (nonatomic, assign) LDFacebookSessionState state;
@property (nonatomic, strong) NSString * accessToken;
@property (nonatomic, strong) NSDate * expirationDate;
@property (nonatomic, strong) NSDictionary * user;
@property (nonatomic, strong) NSArray * permissions;
@end

typedef void (^LDFacebookSessionHandler)(LDFacebookSession * session, NSError * error);
typedef void (^LDFacebookCompletion)(NSDictionary * response, NSError * error);

@class LDFacebookService;

@protocol LDFacebookServiceDelegate<NSObject>
@required
-(void) facebookService:(LDFacebookService *) service didChangeLoginStatus:(LDFacebookSession *) session error:(NSError*) error;
@end

@interface LDFacebookService : NSObject

@property (nonatomic, weak) id<LDFacebookServiceDelegate> delegate;
@property (nonatomic, retain) NSDictionary* params;

+(BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation;
-(void) initialize:(NSDictionary*)params;
-(BOOL) isLoggedIn;
-(void) loginWithReadPermissions:(NSArray *) permissions fromViewController:(UIViewController *) vc completion:(LDFacebookSessionHandler) completion;
-(void) logout;
-(void) getLoginStatus:(BOOL) force completion:(LDFacebookSessionHandler) completion;
-(void) requestAdditionalPermissions:(NSString *) permissionType permissions:(NSArray *) permissions fromViewController:(UIViewController *) vc completion:(LDFacebookSessionHandler) completion;
-(void) api:(NSString *) openGraph method:(NSString*) httpMethod params:(NSDictionary*) params completion:(LDFacebookCompletion) completion;
-(void) ui:(NSString*) methodName params:(NSDictionary*) params  fromViewController:(UIViewController*) vc completion:(LDFacebookCompletion) completion;
-(void) showShareDialog:(NSDictionary*) params fromViewController:(UIViewController*) vc completion:(LDFacebookCompletion)completion;
-(void) uploadPhoto:(NSString*) filePath completion:(LDFacebookCompletion) completion;

@end
