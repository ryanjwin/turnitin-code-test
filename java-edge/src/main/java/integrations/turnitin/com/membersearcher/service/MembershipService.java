package integrations.turnitin.com.membersearcher.service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import integrations.turnitin.com.membersearcher.client.MembershipBackendClient;
import integrations.turnitin.com.membersearcher.model.MembershipList;
import integrations.turnitin.com.membersearcher.model.UserList;
import integrations.turnitin.com.membersearcher.model.Membership;
import integrations.turnitin.com.membersearcher.model.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MembershipService {
	@Autowired
	private MembershipBackendClient membershipBackendClient;

	/**
	 * Method to fetch all memberships with their associated user details included.
	 * This method calls out to the php-backend service and fetches all memberships,
	 * it then calls to fetch the user details for each user individually and
	 * associates them with their corresponding membership.
	 *
	 * @return A CompletableFuture containing a fully populated MembershipList object.
	 */
	public CompletableFuture<MembershipList> fetchAllMembershipsWithUsers() {
		// fetch all memberships
		CompletableFuture<MembershipList> membershipsFuture = membershipBackendClient.fetchMemberships();
		// fetch all users
		CompletableFuture<UserList> usersFuture = membershipBackendClient.fetchUsers();

		// wait for membershipsFuture and usersFuture to finish
		return CompletableFuture.allOf(usersFuture, membershipsFuture)
			.thenApply(nil -> {
				MembershipList membershipsList = membershipsFuture.join();
				UserList usersList = usersFuture.join();
				// lambda function to get user by id
				// could also modify UserList to a map
				Function<String, User> userToInsert =  userId -> {
					for (User user: usersList.getUsers()) {
						if (user.getId().equals(userId)) return user;
					}
					return null;
				};
				// build the membership list
				for (Membership member : membershipsList.getMemberships()) {
					member.setUser(userToInsert.apply(member.getUserId()));
				}
				return membershipsList;
			});
	}
}
