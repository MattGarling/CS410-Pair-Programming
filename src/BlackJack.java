import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class BlackJack {
	private class Card {
		String value;
		String type;

		Card(String value, String type) {
			this.value = value;
			this.type = type;
		}

		public String toString() {
			return value + " of " + type;
		}

		public int getValue() {
			if ("AceJackQueenKing".contains(value)) { // Ace Jack Queen King
				if (value == "Ace") {
					return 11;
				}
				return 10;
			}
			return Integer.parseInt(value); // 2-10
		}

		public boolean isAce() {
			return value == "Ace";
		}

		public String getImagePath() {
			return "cards/" + toString() + ".png";
		}
	}

	private Map<String, Integer> playerChipsMap = new HashMap<>(); // Initialize the playerChipsMap
    private String currentPlayerUsername; // Current player's username
    
    
	ArrayList<Card> deck;
	Random random = new Random(); // shuffle deck

	// dealer
	Card hiddenCard; // the dealer's hidden card
	ArrayList<Card> dealerHand; // what cards the dealer has
	int dealerHandSum; // the sum of the cards in the dealers hand
	int dealerAceCount; // the count of aces in the dealers hand

	// player
	ArrayList<Card> playerHand; // what cards the player has
	int playerHandSum; // the sum of the cards in the players hand
	int playerAceCount; // the count of aces in the players hand
	int playerChips; // the total count of the player's chips
	int playerBet; // the amount of chips the user has bet on the hand

	// window
	int boardWidth = 1000; // set the width of the board
	int boardHeight = 600; // set the height of the board

	int cardWidth = 110; // set the width of the cards
	int cardHeight = 154; // set the height of the cards

	JFrame frame = new JFrame("Black Jack");
	JPanel gamePanel = new JPanel() {
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			try {
				// draw the hidden card
				Image hiddenCardImage = new ImageIcon(getClass().getResource("cards/BACK.png")).getImage();
				if (!standButton.isEnabled()) {
					hiddenCardImage = new ImageIcon(getClass().getResource(hiddenCard.getImagePath())).getImage();
				}
				g.drawImage(hiddenCardImage, 20, 20, cardWidth, cardHeight, null);

				// draw the dealer's cards
				for (int i = 0; i < dealerHand.size(); i++) {
					Card card = dealerHand.get(i);
					Image cardImage = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
					g.drawImage(cardImage, cardWidth + 25 + (cardWidth + 5) * i, 20, cardWidth, cardHeight, null);
				}

				// draw the player's cards
				for (int i = 0; i < playerHand.size(); i++) {
					Card card = playerHand.get(i);
					Image cardImage = new ImageIcon(getClass().getResource(card.getImagePath())).getImage();
					g.drawImage(cardImage, 20 + (cardWidth + 5) * i, 320, cardWidth, cardHeight, null);
				}

				//handle game ending and player's bets
				if (!standButton.isEnabled()) {
					dealerHandSum = reduceDealerAce();
					playerHandSum = reducePlayerAce();

					String message = "";
					
					//both player and user have blackjack
					if(playerHandSum == 21 && dealerHandSum == 21){
						message = "Pushed! " + playerBet + " has been returned";
					} //player has blackjack and the dealer does not 
					else if(playerHandSum == 21 && dealerHandSum != 21) {
						message = "You have Blackjack! You won " + ((playerBet*3)/2) + " chips";
						raisePlayerChips(playerBet, 1.5);
					} //dealer has blackjack and the player does not 
					else if(dealerHandSum == 21 && playerHandSum != 21) {
						message = ("Dealer has Blackjack! You lost " + playerBet + " chips");
						lowerPlayerChips(playerBet);
					} //player busts 
					else if(playerHandSum > 21) {
						message = "You busted! You lost " + playerBet + " chips";
						lowerPlayerChips(playerBet);
					} //dealer busts 
					else if(dealerHandSum > 21) {
						message = "Dealer busted! You won " + playerBet + " chips";
						raisePlayerChips(playerBet, 1);
					}  //player has a greater hand than the dealer
					else if(playerHandSum > dealerHandSum) {
						message = "You won " +  playerBet + " chips";
						raisePlayerChips(playerBet, 1);
					} //dealer has a greater hand than the dealer
					else if(dealerHandSum > playerHandSum) {
						message = "You lost " + playerBet + " chips";
						lowerPlayerChips(playerBet);
					}

					g.setFont(new Font("Arial", Font.PLAIN, 30));
					g.setColor(Color.white);
					g.drawString(message, 220, 250);
					endGame();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	JPanel buttonPanel = new JPanel(); // create a panel for the buttons
	JButton hitButton = new JButton("Hit"); // create a button for the player to hit
	JButton standButton = new JButton("Stand"); // create a button for the player to stay
	JLabel chipsIndicator = new JLabel("Current total: " + playerChips + " Current Bet: " + playerBet);
	JButton playAgainButton = new JButton("Play Again"); //create a button so you can play again
	

	
	private static final String CHIP_TOTAL_FILE = "src/chip_total.txt";

    public void saveChipTotalToFile() {
    	// Append chip balances for all players
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CHIP_TOTAL_FILE, true))) {
            // Append chip balance for the current player
            writer.write(currentPlayerUsername + ":" + playerChips);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadChipTotalFromFile() {
    	// Load chip balances for all players
    	try (BufferedReader reader = new BufferedReader(new FileReader(CHIP_TOTAL_FILE))) {
            String line;

            // Flag to check if the current player's data is found
            boolean currentPlayerFound = false;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String username = parts[0];
                    int chips = Integer.parseInt(parts[1]);

                    playerChipsMap.put(username, chips);

                    // Check if the current player's data is found in the file
                    if (username.equals(currentPlayerUsername)) {
                        playerChips = chips;
                        currentPlayerFound = true;
                    }
                }
            }

            // If the current player's data is not found, set playerChips to 0
            if (!currentPlayerFound) {
                playerChips = 0;
            }

        } catch (IOException | NumberFormatException e) {
            // Handle if the file does not exist (ignore this exception)
            // or if there is an issue parsing the data
            // You may choose to print the stack trace or handle it differently
        }
    }
    
	BlackJack() {
		showUsernameInput(); // Ask for username when the game starts
		
		
		// set the attributes of the frame
		frame.setVisible(true);
		frame.setSize(boardWidth, boardHeight);
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// set the attributes of the game panel
		gamePanel.setLayout(new BorderLayout());
		gamePanel.setBackground(new Color(53, 101, 77));
		frame.add(gamePanel);
		System.out.println("In graphics");
		// set attributes for the hit, stay, and setBet button and the bet text field
		hitButton.setFocusable(false);
		buttonPanel.add(hitButton);
		standButton.setFocusable(false);
		buttonPanel.add(standButton);
		buttonPanel.add(chipsIndicator);
		frame.add(buttonPanel, BorderLayout.SOUTH);

		// handle when the player presses the hit button
		hitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Card card = deck.remove(deck.size() - 1); // get the card the player will get from hitting
				playerHandSum += card.getValue(); // get the value of the card
				playerAceCount += card.isAce() ? 1 : 0; // check if the card is an ace if so increase playerAceCount by 1
				playerHand.add(card); // add the card to the player's hand
				if (reducePlayerAce() > 21) { // if the player is over 21 but has an ace reduce the value from 11 to 1
					hitButton.setEnabled(false);
				}
				gamePanel.repaint();
			}
		});
		
		playAgainButton.setFocusable(false);
        playAgainButton.setEnabled(false); // Initially, disable the "Play Again" button
        buttonPanel.add(playAgainButton);
        playAgainButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetGame(); // Add logic to reset the game when the "Play Again" button is clicked
            }
        });

		// handle when the player presses the stay button
		standButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hitButton.setEnabled(false);
				standButton.setEnabled(false);

				while (dealerHandSum < 17) { // the dealer will only get another card if their card total is below 17
					Card card = deck.remove(deck.size() - 1); // get the card the dealer will get
					dealerHandSum += card.getValue(); // get the value of the card
					dealerAceCount += card.isAce() ? 1 : 0; // check if the card is an ace if so increase dealerAceCount by 1
					dealerHand.add(card); // add the card to the dealer's hand
				}
				gamePanel.repaint();
			}
		});

		gamePanel.repaint();
	}
	
	private void showUsernameInput() {
        currentPlayerUsername = JOptionPane.showInputDialog(null, "Enter your username:");
        if (currentPlayerUsername == null || currentPlayerUsername.trim().isEmpty()) {
            System.exit(0); // Exit the game if the user cancels or enters an empty username
        }

        // Load or create chip balance for the current player
        if (playerChipsMap.containsKey(currentPlayerUsername)) {
            playerChips = playerChipsMap.get(currentPlayerUsername);
        } else {
            playerChips = 1000; // Set an initial balance for new players
            playerChipsMap.put(currentPlayerUsername, playerChips);
        }

        startGame(); // Start the game after obtaining the username
    }

	public void startGame() {
		
		loadChipTotalFromFile(); // Load chip total from file
		if (playerChips == 0) { 
	        showDepositPanel();
	    }
		else {
			setUserBet();
		}
		
		
		//deck
		buildDeck(); //build the deck of cards
		shuffleDeck(); //shuffle the deck of cards
		//setPlayerChips(); //set the players total chips before the hand
		//setUserBet(); //set the user's bet for the hand

		// dealer
		dealerHand = new ArrayList<Card>();
		dealerHandSum = 0; // initialize the sum of the dealer's cards
		dealerAceCount = 0; // initialize the count of aces in the dealer's hand

		// get the dealer's hidden card
		hiddenCard = deck.remove(deck.size() - 1); // remove card at last index
		dealerHandSum += hiddenCard.getValue();
		dealerAceCount += hiddenCard.isAce() ? 1 : 0;

		// get the dealer's one face up card
		Card card = deck.remove(deck.size() - 1);
		dealerHandSum += card.getValue();
		dealerAceCount += card.isAce() ? 1 : 0;
		dealerHand.add(card);

		// player
		playerHand = new ArrayList<Card>();
		playerHandSum = 0; // initialize the sum of the player's cards
		playerAceCount = 0; // initialize the count of aces in the player's hand

		// give the player their two starting cards
		for (int i = 0; i < 2; i++) {
			card = deck.remove(deck.size() - 1);
			playerHandSum += card.getValue();
			playerAceCount += card.isAce() ? 1 : 0;
			playerHand.add(card);
		}
		
	}
	
	public void endGame() {
        playAgainButton.setEnabled(true); // Enable the "Play Again" button when the game ends
       
    }
	
	public void resetGame() {
		playAgainButton.setEnabled(false); // Disable the "Play Again" button after resetting the game
		 playerHand.clear();
		    playerHandSum = 0;
		    playerAceCount = 0;

		    dealerHand.clear();
		    dealerHandSum = 0;
		    dealerAceCount = 0;

		    // Enable hit and stand buttons
		    hitButton.setEnabled(true);
		    standButton.setEnabled(true);

		    startGame(); // Start a new game
		    gamePanel.repaint();
		}
	
	public void resetChips() {
		String depositAmountString = JOptionPane.showInputDialog(null, "Enter the number of chips you want to deposit:");

	    try {
	        int depositAmount = Integer.parseInt(depositAmountString);

	        if (depositAmount >= 10) {
	            playerChips += depositAmount;
	            saveChipTotalToFile(); // Save the updated chip total to the file
	            startGame(); // Start a new game after deposit
	        } else {
	            // Handle invalid deposit amount (less than 10)
	            JOptionPane.showMessageDialog(null, "Invalid deposit amount. Please deposit at least 10 chips.", "Invalid Deposit", JOptionPane.ERROR_MESSAGE);
	            resetChips(); // Show the deposit panel again
	        }
	    } catch (NumberFormatException e) {
	        // Handle if the input is not a valid integer
	        JOptionPane.showMessageDialog(null, "Invalid input. Please enter a valid number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
	        resetChips();// Show the deposit panel again
	    }
	    }
	
	public void showDepositPanel() {
		int option = JOptionPane.showConfirmDialog(null, "You have 0 chips. Would you like to deposit more?", "Deposit Chips", JOptionPane.YES_NO_OPTION);

	    if (option == JOptionPane.YES_OPTION) {
	        resetChips();  // call the resetChips() method to deposit chips
	    } else {
	        System.exit(0); // Terminate the program (you can adjust this based on your requirements)
	    }
	}


	public void buildDeck() {
		deck = new ArrayList<Card>();
		String[] values = { "Ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King" };
		String[] types = { "Clubs", "Diamonds", "Hearts", "Spades" };

		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < values.length; j++) {
				Card card = new Card(values[j], types[i]);
				deck.add(card);
			}
		}
	}

	// method to shuffle the deck
	public void shuffleDeck() {
		for (int i = 0; i < deck.size(); i++) {
			int j = random.nextInt(deck.size());
			Card currCard = deck.get(i);
			Card randomCard = deck.get(j);
			deck.set(i, randomCard);
			deck.set(j, currCard);
		}
	}
	
	/**public void setPlayerChips() {
		playerChips = 50000;
		System.out.println("Player chips: " + playerChips);
		
	} **/

	//get the user's bet
	public void setUserBet() {
		while (true) {
	        String betFieldText = JOptionPane.showInputDialog(null, "Enter your bet (Remaining chips: " + playerChips + "):");

	        try {
	            if (betFieldText == null) {
	                // User clicked cancel or closed the dialog, handle it appropriately
	                // For example, you can exit the game or handle it in another way
	                System.exit(0);
	            } else if (betFieldText.trim().isEmpty()) {
	                throw new NumberFormatException();
	            }

	            // Try to parse the input as an integer
	            playerBet = Integer.parseInt(betFieldText);

	            // Check if the parsed number is within the valid range
	            if (playerBet < 10 || playerBet > 50000 || playerBet > playerChips) {
	                throw new NumberFormatException();
	            }
	            // If everything is valid, break out of the loop
	            break;
	        } catch (NumberFormatException nfe) {
	            // Handle if the input is empty, not a number, or outside the valid range
	            JOptionPane.showMessageDialog(null, "Invalid Bet: Please enter a number between 10 and 50000 and it must be under " + playerChips,
	                    "Invalid Bet", JOptionPane.ERROR_MESSAGE);
	        }
	    }
	    System.out.println("Player1 bet " + playerBet + " chips");
	    
	}

	
	public void lowerPlayerChips(int playerBet) {
		System.out.println("Player chips before lowering " + playerChips);
		playerChips -= playerBet;
		saveChipTotalToFile(); // Save chip total to file after lowering
		System.out.println("Player chips after lowering " + playerChips);
	}
	
	public void raisePlayerChips(int playerBet, double betModifier) {
		System.out.println("Player chips before raising " + playerChips);
		playerChips += (playerBet * betModifier);
		saveChipTotalToFile(); // Save chip total to file after lowering
		System.out.println("Player chips after raising " + playerChips);
	}


	// method to reduce the player's score if they go over 21 and have an ace in their hand
	public int reducePlayerAce() {
		while (playerHandSum > 21 && playerAceCount > 0) {
			playerHandSum -= 10;
			playerAceCount -= 1;
		}
		return playerHandSum;
	}

	// method to reduce the dealer's score if they go over 21 and have an ace in their hand
	public int reduceDealerAce() {
		while (dealerHandSum > 21 && dealerAceCount > 0) {
			dealerHandSum -= 10;
			dealerAceCount -= 1;
		}
		return dealerHandSum;
	}
}
