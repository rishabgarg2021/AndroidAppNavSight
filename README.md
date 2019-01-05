# japan_Navigator
Team Japan for IT project Friday 3:15 pm 
NavSight

(.java files)
AR contains the code need for the AR functionality.

AudioPlayer: Class used to play ringing tone and progressing tone.

BaseActivity:Parent class rspinsible to contain sinch interface object which has listener attached to it which runs in background and is responsible if any call comes.

CallScreenActivity: Screen when you are onan established call.

Chat: It is responsible to show chat window, send message on press of send button, call and ask for help too.

ChatPop: A transparent popup appears on Map with animations applied to it while user is connected to volunteer.It has mostly same functionality as Chat.

Contact:  It creates a Contact Object with userId, name and arraylist of all contact numbers in the phone.

Contacts: It is responsible to display all the contacts fastly through local storage and not accessing everytime through database with invite button sent option too.

DemoUtils: This class is required for the AR to work it provides important UTIL functions that helps implement any AR interface.

Elderly_vol_main: Landing page for elderly when connected with a volunteer. 

elderSelectDestWaiting: Elderly person waiting for volunteer to set the destination.

FavouritePlacesActivity: Shows the favourite places on a list.

FavouritePlacesService: This static service provides services to every class. So if any other class needs to add a fav place they can do using this service.

GenricFileProvider:  A provider used in manifast to grant the permission to access path of the file once image is clicked through camera.

HelperWaiting: Landing page when a helper and volunteer gets connected.

HelpRequests: Elderly sends help request to the volunteer.

IncomingCallScreenActivity: Pop-up screen when you receive a call.

IncomingHelpRequest: Pop-up screen when you check the notification for a help request.

LocalContactsStorage: This class maintains all the locally stored contacts and implements LocalStorageService.

LocalPlacesStorage: This class maintains all the locally stored places and implements LocalStorageService.

LocalStorageService: This is an interface for every Locally stored object needs to implement this.

Login: It helps to authenticate user through phone, genrate OTP and verify.

LookupPlaceActivity: Search the place on maps automatically when clicked.

Main_Menu_Map: Shows the main map with routes and the menu.

Main_Activity: it helps to create a splash screen and load the stuff from database and redirect to login, userInfo or Main Activity depending on user profile created tilll the installation of app.

MessageAdapter: A Adapter pattern with recycler view attahced to messages.Responsible to assign a view, configre view to diiferent chat message  window on either side.

Messages: It helps to create a Message Object sent each time the message is sent.

mPlace: class which contains information about a place

MyFirebaseMessagingService:  Receives notifications from firebase and builds notifications

selectDestinationToShare:   Allows elderly or volunteer to enter the destination for the elderly or allow remote access.

showDestinationRoute : Shows the route from the elderly to a destination.

showVolunteerRoute: Map which shows the route for volunteer and elder to meet up.

SinchService: Calling API which receives calls and controls the client/

User: A user object is created when a user is first time creating it’s profile.

UserConnection:  Class which holds all the information necessary if a a volunteer-elderly connection gets closed accidentally.

userInfo: Helps to show resister activty in which user uploads it’s profile image, name, phone number and emergency contact number.

volSelectDestWaiting:  Volunteer waiting for the elderly to enter the destination.

volunteerConnect: List of all the closest volunteers.

volunteerProfile: Displays profile of a volunteer

(.js file)
Index: Contains 2 functions which is used to pass notifications from firebase onto a device.

/******************************************************************************************************/

Testing Modules:
Instrumented Tests (app\src\androidTest\java\com\helpyou\itproject)

AudioPlayerTest: This is an Instrumented test which will test the Audio protocol of the device and if any sound made runs or not.

Main_Menu_MapTest: This test will ensure that the volunteer mode is working fine and google Search api is being displayed.

ContactsTest: This will test the entire Contacts UI Class including opening up the Chat box page.

DemoUtilsTest: DemoUtils is required by the AR to work. If DemoUtil works AR has to work using the API.

LocalContactStorageTest: This tests if the Contacts can be stored and extracted locally or not.

LoginTest: This tests the login authentication is working at all times or not.

LocalPlacesStorageTest: This tests if the Favourite places can be stored locally or not.

Non-Instrumented Tests (app\src\test\java\com\helpyou\itproject)

MessagesTest : This is mostly a sample Junit test to see if messages are created and can be extracted.

/******************************************************************************************************/

